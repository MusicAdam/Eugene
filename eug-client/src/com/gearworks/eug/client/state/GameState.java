package com.gearworks.eug.client.state;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.PlayerState;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.Simulator;
import com.gearworks.eug.shared.events.EntityEventListener;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.state.State;
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
	
	private Simulator simulator;
	private int assignInstanceMessageIndex = -1;
	private int initializeSceneMessageIndex = -1;
	private int serverUpdateMessageIndex = -1;
	private int playerInputMessageIndex = -1;
	private EntityEventListener entityEventListener;
	private int tick;
	private Snapshot latestSnapshot;
	private int updatesUntilForceCorrection = 10; //TODO: This should be a a config variable. Number of updates we will wait for a server correction on input until we accept any server snapshot.
	private int updatesSinceLastCorrection = 0;	//Counter for updatesUntilForceCorrection
	
	private long startTime;
	private long endTime;
	private long frameStart = 0;
	private long frameTimeSum = 0;
	
	
	private ConcurrentLinkedQueue<Snapshot> serverUpdateQueue = new ConcurrentLinkedQueue<Snapshot>();

	@Override
	public boolean canEnterState() {
		return EugClient.GetPlayer() != null && EugClient.GetPlayer().isConnected();
	}

	@Override
	public void onEnter() {
		Debug.println("[GameState: onEnter()]");
		startTime = Utils.generateTimeStamp();
		
		//Register assigninstancemessage to wait for our instance to be assigned.
		assignInstanceMessageIndex = EugClient.GetMessageRegistry().listen(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				EugClient.SetInstance(aMsg.getInstanceId());
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
		Eug.GetWorld().removeEntityListener(entityEventListener);
		
		if(assignInstanceMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(assignInstanceMessageIndex);
		if(initializeSceneMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(initializeSceneMessageIndex);
		if(serverUpdateMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(serverUpdateMessageIndex);
		if(playerInputMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(playerInputMessageIndex);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void update() {
		frameStart = Utils.generateTimeStamp();
		
		Eug.GetWorld().update(SharedVars.STEP);

		if(EugClient.GetPlayer().isValid()){ //Update entities			
			//Initialize simulator if it hasn't been already
			if(simulator == null){
				simulator = new Simulator();
			}		
			
			//Remove init scene message and registry update message if it still needs to be done.
			if(initializeSceneMessageIndex != -1){
				EugClient.GetMessageRegistry().remove(initializeSceneMessageIndex);
				initializeSceneMessageIndex = -1;
				
				final GameState thisRef = this; 
				serverUpdateMessageIndex = 	
						EugClient.GetMessageRegistry().listen(UpdateMessage.class, new MessageCallback(){
							@Override
							public void messageReceived(Connection c, Message msg){
								thisRef.serverUpdate((UpdateMessage)msg);
							}
						});
				playerInputMessageIndex =
						EugClient.GetMessageRegistry().listen(PlayerInput.class, new MessageCallback(){
							@Override
							public void messageReceived(Connection c, Message msg){
								//System.out.println("PlayerInput response"); // Do nothing
							}
						});
			}
		}else if(EugClient.GetPlayer().isInstanceValid() && !EugClient.GetPlayer().isInitialized()){
			//Remove the assigninstancemessage callback if it still exists & create sceneinitialize listener
			if(assignInstanceMessageIndex != -1){
				EugClient.GetMessageRegistry().remove(assignInstanceMessageIndex);
				assignInstanceMessageIndex = -1;
				
				final GameState thisRef = this; 
				initializeSceneMessageIndex = EugClient.GetMessageRegistry().listen(InitializeSceneMessage.class, new MessageCallback(){
					@Override
					public void messageReceived(Connection c, Message msg){
						thisRef.initializeScene(c, (InitializeSceneMessage)msg);
					}
				});
			}
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
		EugClient.GetPlayer().setInitialized(true);
		EugClient.GetWorld().setRecordHistory(true);
		
		Snapshot snapshot = msg.getSnapshot();		
		//latestServerSnapshot = snapshot;
		
		Eug.GetWorld().snapToSnapshot(snapshot);
		
		InitializeSceneMessage ack = new InitializeSceneMessage(EugClient.GetPlayer().getInstanceId(), null);
		ack.sendUDP(EugClient.GetPlayer().getConnection());
		
		Debug.println("[EugClient:initializeScene] Scene initialized and response sent.");
		System.out.println("[EugClient:initializeScene] Server at tick " + snapshot.getTick() + ", client at " + Eug.GetWorld().getTick());
	}
	
	protected void serverUpdate(UpdateMessage msg) {
		if(!EugClient.GetPlayer().isValid()) return;
		
		Snapshot serverSnapshot = msg.getSnapshot();
		
		Eug.GetWorld().synchronizeSnapshot(serverSnapshot);
		if(true)return ;
		
		//System.out.println("[serverUpdate] Server at tick " + serverSnapshot.getTick() + ", client at " + Eug.GetWorld().getTick());
		
		if(EugClient.GetPlayer().getInputs().size() > 0){
			boolean shouldCorrect = false;
			for(PlayerInput serverInput : serverSnapshot.getInput()){
				Iterator<PlayerInput> iterator = EugClient.GetPlayer().getInputs().iterator();
				
				while(iterator.hasNext()){
					PlayerInput clientInput = iterator.next();
										
					if(!clientInput.isSaved()){
						iterator.remove();//If this input is not saved, don't wait for it and remove it
					}else if(serverInput.getTimestamp() == clientInput.getTimestamp() &&
							 serverInput.getTargetPlayerID() == clientInput.getTargetPlayerID()){ 
						
						clientInput.setCorrected(true);
						iterator.remove();
						
						shouldCorrect = true;
					}
					
					if(updatesSinceLastCorrection >= updatesUntilForceCorrection){
						shouldCorrect = true;
						iterator.remove();
					}
				}
			}
			
			if(!shouldCorrect){
				updatesSinceLastCorrection++;
				System.out.println("[serverUpdate] waiting on correction ");
				return; //Still waiting for corrected snapshot, dont want to interrupt our prediction with invalid server states.	
			}
		}		

		updatesSinceLastCorrection = 0;
				
		Eug.GetWorld().pruneHistory(msg.getSnapshot().getTimestamp());
		Snapshot localSnapshot =  	Eug.GetWorld().getHistory().peek();
		
		//Queue the server snapshot because we haven't completed a full frame since the last update
		if(localSnapshot == null)
			localSnapshot = Eug.GetWorld().getLatestSnapshot();
		
		
		long stepMs = (long)(SharedVars.STEP * 1000);
		
		//Check if our history is within 1 step of the server. If it is not, we are out of sync
		//and should snap to server
		if(	Utils.timeCompareEpsilon(localSnapshot.getTimestamp(), serverSnapshot.getTimestamp(), stepMs)){
			Eug.GetWorld().snapToSnapshot(serverSnapshot);
			return;
		}
						
		if(!Snapshot.Compare(serverSnapshot, localSnapshot)){	//If the serverSnapshot and the localSnapshot don't match, calculate a corrected state
			Snapshot result = simulator.simulate(serverSnapshot, Eug.GetWorld().getLatestSnapshot().getTimestamp(), Eug.GetWorld().getHistory());
			Eug.GetWorld().snapToSnapshot(result);
		}
	}
	
	public int getTick(){ return tick; }
	
	@Override
	public int getId() {
		return 1;
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
