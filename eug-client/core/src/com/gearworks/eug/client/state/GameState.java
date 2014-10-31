package com.gearworks.eug.client.state;

import java.nio.Buffer;
import java.util.Iterator;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.utils.BufferUtils;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.EntityEventListener;
import com.gearworks.eug.shared.EntityManager;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityUpdateException;
import com.gearworks.eug.shared.input.ClientInput;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.state.State;
import com.gearworks.eug.shared.utils.CircularBuffer;
import com.gearworks.eug.shared.utils.Utils;


/*
 * Encapsulates actual gameplay logic. 
 *  - Handles instance initialization
 *  - Handles scene initialization
 *  - Handles server updates
 */
public class GameState implements State {
	public static float POSITION_SMOOTHING_MAXIMUM = 1.0f; //2 meters
	public static float POSITION_SMOOTHING_RATIO = .01f; //10th of the total distance (will be scaled by latency)
	public static float POSITION_SMOOTHING_MINIMUM = 0; //Minimum delta which will be smoothed
	public static float ROTATION_SMOOTHING_MAXIMUM = Utils.degToRad(40);
	public static float ROTATION_SMOOTHING_RATIO = .01f;
	public static float ROTATION_SMOOTHING_MINIMUM = .1f; //Minimum delta which will be smoothed
	
	private int assignInstanceMessageIndex = -1;
	private int initializeSceneMessageIndex = -1;
	private int serverUpdateMessageIndex = -1;
	private EntityEventListener entityEventListener;
	private long latency;
	private CircularBuffer<ClientInput> inputHistory;
	private CircularBuffer<Snapshot> snapshotHistory;
	private int tick;
	private Snapshot latestSnapshot;
	private Snapshot latestServerSnapshot;
	private Snapshot latestSimulatedSnapshot;
	private int forceResetLimit = 2; //Updates until we decide correction has failed and we need to snap to server state.
	private int forceResetCount = 0; //Number of updates we remain out of sync
	
	private long startTime;
	private long endTime;
	private long frameStart = 0;
	private long frameTimeSum = 0;

	@Override
	public boolean canEnterState() {
		return EugClient.GetPlayer() != null && EugClient.GetPlayer().isConnected();
	}

	@Override
	public void onEnter() {
		Debug.println("[GameState: onEnter()]");
		startTime = Utils.generateTimeStamp();
		inputHistory = new CircularBuffer<ClientInput>(SharedVars.HISTORY_SIZE);
		snapshotHistory = new CircularBuffer<Snapshot>(SharedVars.HISTORY_SIZE);
		
		//Register assigninstancemessage to wait for our instance to be assigned.
		assignInstanceMessageIndex = EugClient.GetMessageRegistry().register(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				EugClient.SetInstance(aMsg.getInstanceId());
			}
		});		
		
		//Initialize entity listeners
		entityEventListener = EntityManager.AddListener(new EntityEventListener(){
			@Override
			public void onCreate(Entity ent){
				//Debug.println("Created " + ent.getId());
				System.out.println("Created entity " + ent.getId() + " at " + Utils.generateTimeStamp());
				if(ent instanceof DiskEntity){
					if(ent.getPlayer() == EugClient.GetPlayer()){
						EugClient.GetPlayer().setDisk((DiskEntity)ent);
					}
				}
			}
			
			@Override
			public void onDestroy(Entity ent){
				System.out.println("Destroyed entity " + ent.getId() + " at " + Utils.generateTimeStamp());
			}
		});
	}

	@Override
	public boolean canExitState() {
		return true;
	}

	@Override
	public void onExit() {
		//Cleanup all our callbacks/listeners
		EntityManager.RemoveListener(entityEventListener);
		
		if(assignInstanceMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(assignInstanceMessageIndex);
		if(initializeSceneMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(initializeSceneMessageIndex);
		if(serverUpdateMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(serverUpdateMessageIndex);
	}

	@Override
	public void render() {
		if(EugClient.GetPlayer().isValid()){ //Render entities
			Iterator entIt = EugClient.GetEntities().entrySet().iterator();
			while(entIt.hasNext())
			{ 
				Map.Entry<Integer, Entity> pairs = (Map.Entry<Integer, Entity>)entIt.next();
				pairs.getValue().render(EugClient.GetSpriteBatch(), EugClient.GetShapeRenderer());
			}
		}
		
		if(latestServerSnapshot != null){
			Color old = EugClient.GetSpriteBatch().getColor();
			EugClient.GetSpriteBatch().setColor(1, 0, 0, .8f);
			latestServerSnapshot.render(EugClient.GetSpriteBatch());
			EugClient.GetSpriteBatch().setColor(old);
		}
		
		if(latestSimulatedSnapshot != null){
			Color old = EugClient.GetSpriteBatch().getColor();
			EugClient.GetSpriteBatch().setColor(0, 1, 0, .8f);
			latestSimulatedSnapshot.render(EugClient.GetSpriteBatch());
			EugClient.GetSpriteBatch().setColor(old);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void update() {
		frameStart = Utils.generateTimeStamp();
		latestSnapshot = EntityManager.GenerateSnapshot(EugClient.GetInstanceId());
		snapshotHistory.push(latestSnapshot);
		
		if(EugClient.GetPlayer().isValid()){ //Update entities
			//Remove init scene message and registry update message if it still needs to be done.
			if(initializeSceneMessageIndex != -1){
				EugClient.GetMessageRegistry().remove(initializeSceneMessageIndex);
				initializeSceneMessageIndex = -1;
				
				final GameState thisRef = this; 
				serverUpdateMessageIndex = 	
						EugClient.GetMessageRegistry().register(UpdateMessage.class, new MessageCallback(){
							@Override
							public void messageReceived(Connection c, Message msg){
								thisRef.serverUpdate((UpdateMessage)msg);
							}
						});
			}
			
			Iterator entIt = EugClient.GetEntities().entrySet().iterator();
			while(entIt.hasNext())
			{ 
				Map.Entry<Integer, Entity> pairs = (Map.Entry<Integer, Entity>)entIt.next();
				pairs.getValue().update();
			}
		}else if(EugClient.GetPlayer().isInstanceValid() && !EugClient.GetPlayer().isInitialized()){
			//Remove the assigninstancemessage callback if it still exists & create sceneinitialize listener
			if(assignInstanceMessageIndex != -1){
				EugClient.GetMessageRegistry().remove(assignInstanceMessageIndex);
				assignInstanceMessageIndex = -1;
				
				final GameState thisRef = this; 
				initializeSceneMessageIndex = EugClient.GetMessageRegistry().register(InitializeSceneMessage.class, new MessageCallback(){
					@Override
					public void messageReceived(Connection c, Message msg){
						thisRef.initializeScene(c, (InitializeSceneMessage)msg);
					}
				});
			}
		}
		
		Eug.GetWorld().step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
		
		tick++;
		//Handle wrapping
		if(tick < 0){
			tick = 0;
		}
		
		frameTimeSum += (Utils.generateTimeStamp() - frameStart);
	}
	
	protected void initializeScene(Connection c, InitializeSceneMessage msg) {
		if(!EugClient.GetPlayer().isInstanceValid()) return;
		if(EugClient.GetPlayer().isInitialized()){
			//If for some reason we are still getting this message after we have been initialized and we are initialize, let the server know
			InitializeSceneMessage ack = new InitializeSceneMessage(EugClient.GetPlayer().getInstanceId(), null);
			EugClient.GetPlayer().getConnection().sendUDP(ack);
			Debug.println("[GameState:initializeScene] Redundant Scene initialized response sent.");	
			return;
		}
		
		Snapshot snapshot = msg.getSnapshot();		
		latestServerSnapshot = snapshot;
		
		EugClient.SynchronizePlayers(snapshot);
		
		for(EntityState state : snapshot.getEntityStates()){
			try {
				EntityManager.BuildFromState(state);
				/*
				 * TODO: Handle these exceptions in a friendlier way. (Disconnect player and return to main menu? Retry?)
				 */
			} catch (EntityBuildException e) {
				e.printStackTrace();
			} catch (EntityUpdateException e) {
				e.printStackTrace();
			}
		}
		
		EugClient.GetPlayer().setInitialized(true);
		InitializeSceneMessage ack = new InitializeSceneMessage(EugClient.GetPlayer().getInstanceId(), null);
		ack.sendUDP(EugClient.GetPlayer().getConnection());
		
		Debug.println("[EugClient:initializeScene] Scene initialized and response sent.");
	}
	
	protected void serverUpdate(UpdateMessage msg) {
		if(!EugClient.GetPlayer().isValid()) return;
		
		Snapshot serverSnapshot = msg.getSnapshot();
		latestServerSnapshot = serverSnapshot;
		
		latency = msg.getTravelTime();

		EugClient.SynchronizePlayers(serverSnapshot);

		//Get rid of history before the related state
		//pruneOldHistory(serverSnapshot.getTimestamp());
		
		//This gets rid of all saved snapshots from before the latest server snapshot.
		while(snapshotHistory.peek() != null && snapshotHistory.peek().getTimestamp() < serverSnapshot.getTimestamp()){
			snapshotHistory.pop();
		}
		
		while(inputHistory.peek() != null && inputHistory.peek().getTimestamp() < serverSnapshot.getTimestamp()){
			inputHistory.pop();
		}
		
		
		Snapshot simulatedState = null;
		Snapshot localSnapshot = snapshotHistory.peek(); 
		
		if(localSnapshot == null)
		{
			localSnapshot = getLatestSnapshot(); //If we are in sync with the server, get the latest snapshot instead.
		}
		
		if(localSnapshot.getTimestamp() < serverSnapshot.getTimestamp()){
			localSnapshot = EntityManager.SimulateTo(localSnapshot, serverSnapshot.getTimestamp(), inputHistory, snapshotHistory, getLatestSnapshot());
		}else if(localSnapshot.getTimestamp() > serverSnapshot.getTimestamp()){
			serverSnapshot = EntityManager.SimulateTo(serverSnapshot, localSnapshot.getTimestamp(), inputHistory, snapshotHistory, getLatestSnapshot());
		}
		
		if(!Snapshot.Compare(serverSnapshot, localSnapshot)){	
			simulatedState = EntityManager.SimulateTo(serverSnapshot, getLatestSnapshot().getTimestamp(), inputHistory, snapshotHistory, getLatestSnapshot());
			
			if(simulatedState != null){
				if(!Snapshot.Compare(simulatedState, getLatestSnapshot())){
					latestSimulatedSnapshot = simulatedState;
					try {
						EntityManager.SynchronizeEntities(simulatedState);
						EntityManager.SnapToState(simulatedState.getEntityStates());
					} catch (EntityUpdateException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
	}

	//Remove all input/snapshots older than t
	private void pruneOldHistory(long t){
		while(inputHistory.peek() != null && inputHistory.peek().getTimestamp() < t){
			inputHistory.pop();
		}
		while(snapshotHistory.peek() != null && snapshotHistory.peek().getTimestamp() < t){
			snapshotHistory.pop();
		}
	}
	
	public int getTick(){ return tick; }
	
	@Override
	public int getId() {
		return 1;
	}

	public void storeMove(ClientInput input) {
		inputHistory.push(input);
	}

	
	
	public Snapshot getLatestSnapshot(){
		return latestSnapshot;
	}

	@Override
	public void dispose() {
		endTime = Utils.generateTimeStamp();
		
		long runtime = (endTime - startTime);
		float avgFrame = (float)frameTimeSum / runtime;
		Debug.log("[GameState:dispose] Runtime:\t" + runtime);
		Debug.log("[GameState:dispose] Average frame time:\t" + avgFrame);
	}

}
