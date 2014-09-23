package com.gearworks.eug.client.state;

import java.nio.Buffer;

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
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.InputSnapshot;
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
	private CircularBuffer<InputSnapshot> inputHistory;
	private int tick;
	private Snapshot latestSnapshot;

	@Override
	public boolean canEnterState() {
		return EugClient.GetPlayer() != null && EugClient.GetPlayer().isConnected();
	}

	@Override
	public void onEnter() {
		Debug.println("[GameState: onEnter()]");
		inputHistory = new CircularBuffer<InputSnapshot>(SharedVars.HISTORY_SIZE);
		
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
				if(ent instanceof DiskEntity){
					if(ent.getPlayer() == EugClient.GetPlayer()){
						EugClient.GetPlayer().setDisk((DiskEntity)ent);
					}
				}
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
			for(Entity e : EugClient.GetEntities())
			{ 
				e.render(EugClient.GetSpriteBatch(), EugClient.GetShapeRenderer());
			}
		}
	}

	@Override
	public void update() {
		latestSnapshot = generateSnapshot();
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
			
			for(Entity e : EugClient.GetEntities())
			{ 
				e.update();
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
		tick = snapshot.getTick();
		
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
		EugClient.GetPlayer().getConnection().sendUDP(ack);
		
		Debug.println("[EugClient:initializeScene] Scene initialized and response sent.");
	}
	
	protected void serverUpdate(UpdateMessage msg) {
		if(!EugClient.GetPlayer().isValid()) return;
		
		ServerState state = msg.getState();
		Snapshot snapshot = state.getSnapshot();
		
		latency = (Utils.generateTimeStamp() - snapshot.getTimestamp());

		EugClient.UpdatePlayers(state.getPlayerIds(), state.getDisconnectedPlayers());
		for(int i = 0; i < snapshot.getEntityStates().length; i++){
			try {
				EntityState entState = snapshot.getEntityStates()[i];
				if(entState.getPlayerId() == EugClient.GetPlayer().getId() && state.getInput() != null){
					//Apply correction
					int tick = state.getInput().getTick();
					int currentTick = getTick();
					Snapshot currentState = new Snapshot(EugClient.GetInstanceId(), getEntityStates());
					EntityState correctedState = null; //Where we will store the corrected state of the client disk
					pruneOldHistory(state.getInput().getTick());
					
					//Set state to state at which input occured
					System.out.println(inputHistory.peek());
					EntityManager.SnapToState(inputHistory.peek().getSnapshot().getEntityStates());
					
					while(tick < currentTick && !inputHistory.isEmpty()){
						if(inputHistory.peek().getTick() == tick){
							InputSnapshot input = inputHistory.pop();
							input.resolve(EugClient.GetPlayer().getDisk());
						}
						//Simulate
						EugClient.GetWorld().step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
						tick++;
					}
					
					correctedState = EugClient.GetPlayer().getDisk().getState();
					
					//Revert back to current state
					EntityManager.SnapToState(currentState.getEntityStates());
					
					//Interpolate current state to corrected state
					EntityManager.InterpolateToState(correctedState, EugClient.GetPlayer().getDisk());					
				}else{
					EntityManager.UpdateToState(entState, true);					
				}
			} catch (EntityBuildException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EntityUpdateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		/*
		if(snapshot.getTick() < getTick()){ //Server is behind
			pruneOldHistory(getTick());
			if(!snapshot.isEmpty())
				snapshot = snapshotHistory.pop();
		}
		
		
		*/
	}
	
	private EntityState[] getEntityStates() {
		EntityState[] states = new EntityState[Eug.GetEntities().size];
		for(int i = 0; i < states.length; i++){
			states[i] = Eug.GetEntities().get(i).getState();
		}
		return states;
	}

	//Remove all input/snapshots older than t
	private void pruneOldHistory(int t){
		InputSnapshot input = null;
		while(inputHistory.peek() != null && inputHistory.peek().getTick() < t){
			inputHistory.pop();
		}
	}
	
	public int getTick(){ return tick; }
	
	@Override
	public int getId() {
		return 1;
	}

	public void storeMove(InputSnapshot input) {
		inputHistory.push(input);
	}

	private Snapshot generateSnapshot() {
		return new Snapshot(EugClient.GetInstanceId(), getEntityStates());
	}
	
	public Snapshot getSnapshot(){
		return latestSnapshot;
	}

}
