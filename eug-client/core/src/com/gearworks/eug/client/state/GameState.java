package com.gearworks.eug.client.state;

import java.nio.Buffer;
import java.util.Iterator;
import java.util.Map;

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
import com.gearworks.eug.shared.state.ServerState;
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
				//Debug.println("Destroyed " + ent.getId());
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
		
		Snapshot snapshot = msg.getState().getSnapshot();		
		tick = snapshot.getServerTick();
		
		EugClient.UpdatePlayers(msg.getState().getPlayerIds(), msg.getState().getDisconnectedPlayers());
		
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
		if(msg.getState().getSnapshot().getServerTick() > getTick()) return;
		
		ServerState state = msg.getState();
		Snapshot serverSnapshot = state.getSnapshot();
		
		latency = msg.getTravelTime();

		EugClient.UpdatePlayers(state.getPlayerIds(), state.getDisconnectedPlayers());

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
		
			
		System.out.println("Local closest snapshot time: " + Utils.timeToString(localSnapshot.getTimestamp()));
		System.out.println("Server snapshot time: " + Utils.timeToString(serverSnapshot.getTimestamp()));
		
		if(Snapshot.Compare(serverSnapshot, localSnapshot)){
			//System.out.println("CLIENT/SERVER ARE REASONABLY IN SYNC");
		}else{
			System.out.println("CLIENT/SERVER OUT OF SYNC");
			if(localSnapshot.getTimestamp() > serverSnapshot.getTimestamp()){
				//If we are ahead of the server, simulate the server state to our current state, and compare.
				simulatedState = EntityManager.SimulateTo(serverSnapshot, localSnapshot, inputHistory, snapshotHistory, getLatestSnapshot());
			}else if(localSnapshot.getTimestamp() < serverSnapshot.getTimestamp()){
				//If we are behind the server, simulate our state to the server's state and compare.
				simulatedState = EntityManager.SimulateTo(serverSnapshot, localSnapshot, inputHistory, snapshotHistory, getLatestSnapshot());
				
			}else{
				//If we are equal to the server state compare current state.
				simulatedState = getLatestSnapshot();
			}
			
			try {
				EntityManager.SnapToState(simulatedState.getEntityStates());
			} catch (EntityUpdateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		
		
		
						
		/*
		for(int i = 0; i < serverSnapshot.getEntityStates().length; i++){
			try {
				EntityState serverEntState = serverSnapshot.getEntityStates()[i];
				Entity ent = Eug.FindEntityById(serverEntState.getId());
				boolean newEntity = false;
				if(ent == null){
					ent = EntityManager.BuildFromState(serverEntState);
					newEntity = true;
					System.out.println("NEW ENTITY");
				}
					
				//Compare the server's entity state with the local entity state if it is not new
				if(!newEntity){
					System.out.println("Getting entity state: " + serverEntState.getId());
					EntityState oldState = snapshotHistory.peek().getEntityState(serverEntState.getId()); //Get the state of the entity at the time the server took a snapshot.
					if(!snapshotHistory.isEmpty() && oldState != null && EntityState.Compare(serverEntState, oldState)) continue; //Don't correct if no correction is required
				}
				
				//Perform correction if a new entity was created, or if there has been input 
				if(serverEntState.getPlayerId() == EugClient.GetPlayer().getId() && !snapshotHistory.isEmpty() && !inputHistory.isEmpty() || newEntity){
					//Set the state to the server's state when the input occured.
					EntityManager.SnapToState(snapshotHistory.pop().getEntityStates());
					
					if(newEntity){
						EntityManager.BuildFromState(serverEntState);
					}
					
					long simTime = serverSnapshot.getTimestamp();
					long currentTime = Utils.generateTimeStamp();
					EntityState correctedState = null; //Where we will store the corrected state of the client disk
					
					//Simulate to now using the corrected server state and the client inputs
					System.out.println("Simulating..");
					while(simTime < currentTime){
						System.out.println("\t" + simTime + " < " + currentTime);
						if(!inputHistory.isEmpty() && inputHistory.peek().getTimestamp() <= simTime){ //Apply relevant inputs
							ClientInput input = inputHistory.pop();
							input.resolve();
						}
						//Simulate
						EugClient.GetWorld().step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
						simTime += SharedVars.STEP * 1000;
					}
					
					//Get the current (corrected) state of the player disk.
					correctedState = EugClient.GetEntities().get(serverEntState.getId()).getState();
					
					
					//Revert back to current state
					EntityManager.SnapToState(generateSnapshot().getEntityStates());
				}else{
					EntityManager.SnapToState(serverEntState);	
					System.out.println("SNAP");
					//EntityManager.UpdateToState(serverEntState, false, 0);					
				}
			} catch (EntityUpdateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EntityBuildException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		//Save the server's state for correction later.
		serverSnapshot.setServerTick(getTick());
		/*
		if(snapshot.getTick() < getTick()){ //Server is behind
			pruneOldHistory(getTick());
			if(!snapshot.isEmpty())
				snapshot = snapshotHistory.pop();
		}
		
		
		*/
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
