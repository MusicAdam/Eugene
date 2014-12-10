package com.gearworks.eug.client.state;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.EntityEventListener;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.PlayerState;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.Simulator;
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
			Snapshot correctedState = null;
			if((correctedState = simulator.getResult()) != null &&
				EugClient.GetPlayer().getInputs().size() == 0){
				Eug.GetWorld().snapToSnapshot(correctedState);
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
		
		Snapshot snapshot = msg.getSnapshot();		
		//latestServerSnapshot = snapshot;
		
		Eug.GetWorld().synchronizeSnapshot(snapshot);
		
		EugClient.GetPlayer().setInitialized(true);
		InitializeSceneMessage ack = new InitializeSceneMessage(EugClient.GetPlayer().getInstanceId(), null);
		ack.sendUDP(EugClient.GetPlayer().getConnection());
		
		Debug.println("[EugClient:initializeScene] Scene initialized and response sent.");
	}
	
	protected void serverUpdate(UpdateMessage msg) {
		if(!EugClient.GetPlayer().isValid()) return;
				
		Snapshot serverSnapshot = msg.getSnapshot();
		
		if(EugClient.GetPlayer().getInputs().size() > 0){
			boolean shouldCorrect = false;
			for(PlayerInput serverInput : serverSnapshot.getInput()){
				Iterator<PlayerInput> iterator = EugClient.GetPlayer().getInputs().iterator();
				
				while(iterator.hasNext()){
					PlayerInput clientInput = iterator.next();
					
					if(updatesSinceLastCorrection >= updatesUntilForceCorrection){
						shouldCorrect = true;
						iterator.remove();
					}
					
					if(!clientInput.isSaved()){
						iterator.remove();//If this input is not saved, don't wait for it and remove it
					}else if(serverInput.getTimestamp() == clientInput.getTimestamp() &&
					   serverInput.getTargetPlayerID() == clientInput.getTargetPlayerID()){ 
						
						clientInput.setCorrected(true);
						iterator.remove();
						
						shouldCorrect = true;
					}
				}
			}
			
			if(!shouldCorrect){
				updatesSinceLastCorrection++;
				return; //Still waiting for corrected snapshot, dont want to interrupt our prediction with invalid server states.	
			}
		}		

		updatesSinceLastCorrection = 0;

		Eug.GetWorld().synchronizeSnapshot(serverSnapshot);
		
		//Since synchronizeSnapshot only accounts for the creation/deletion of new/old players & entities, we need to sync the state of players and entities ourselves.
		for(PlayerState plState : serverSnapshot.getPlayers()){
			Player pl = Eug.FindPlayerById(plState.getId());
			
			if(!pl.getState().equals(plState)){
				pl.snapToState(plState);
			}
		}		
		
		if(simulator.isRunning()) simulator.terminateSimulation(); //Don't care about the old update, we have new data
		
	
		Snapshot localSnapshot = Eug.GetWorld().pruneHistory(msg.getSnapshot().getTimestamp()); 	
		
		//Queue the server snapshot because we haven't completed a full frame since the last update
		if(localSnapshot == null)
			localSnapshot = Eug.GetWorld().getLatestSnapshot();

		
		if(localSnapshot.getTimestamp() < serverSnapshot.getTimestamp()){ //If our local history is behind the server, simulate to where the server is.
			simulator.simulate(localSnapshot, serverSnapshot.getTimestamp(), Eug.GetWorld().getHistory());
			
			//Wait for the simulation to complete
			while(simulator.isRunning()){} 			
					
			localSnapshot = simulator.getResult();
		}else if(localSnapshot.getTimestamp() > serverSnapshot.getTimestamp()){ //If our local history is ahead of the server, simulate server to where we are
			simulator.simulate(serverSnapshot, localSnapshot.getTimestamp(), Eug.GetWorld().getHistory());
			
			//Wait for simulation to complete
			while(simulator.isRunning()){}
			
			serverSnapshot = simulator.getResult();
		}
				
		if(!Snapshot.Compare(serverSnapshot, localSnapshot)){	//If the serverSnapshot and the localSnapshot don't match, calculate a corrected state
			simulator.simulate(serverSnapshot, Eug.GetWorld().getLatestSnapshot().getTimestamp(), Eug.GetWorld().getHistory());
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
