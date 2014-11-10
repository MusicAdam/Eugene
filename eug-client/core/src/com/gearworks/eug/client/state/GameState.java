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
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.PlayerState;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.Simulator;
import com.gearworks.eug.shared.World;
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
	
	private Simulator simulator;
	private int assignInstanceMessageIndex = -1;
	private int initializeSceneMessageIndex = -1;
	private int serverUpdateMessageIndex = -1;
	private EntityEventListener entityEventListener;
	private long latency;
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
	
	private DiskEntity testEnt;

	@Override
	public boolean canEnterState() {
		return EugClient.GetPlayer() != null && EugClient.GetPlayer().isConnected();
	}

	@Override
	public void onEnter() {
		Debug.println("[GameState: onEnter()]");
		startTime = Utils.generateTimeStamp();
		
		//Register assigninstancemessage to wait for our instance to be assigned.
		assignInstanceMessageIndex = EugClient.GetMessageRegistry().register(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				EugClient.SetInstance(aMsg.getInstanceId());
			}
		});		
		
		//Initialize entity listeners
		/*entityEventListener = Eug.GetWorld().addEntityListener(new EntityEventListener(){
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
		});*/
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

	@Override
	public void render() {		
		EugClient.GetWorld().render();
		
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
		
		Eug.GetWorld().update(SharedVars.STEP);

		if(EugClient.GetPlayer().isValid()){ //Update entities
			//Add inputs if there are any
			for(ClientInput input : EugClient.GetPlayer().getInputs()){
				latestSnapshot.pushInput(input);
			}
			EugClient.GetPlayer().clearInputs();
			
			//Initialize simulator if it hasn't been already
			if(simulator == null){
				simulator = new Simulator();
			}		
			Snapshot correctedState = null;
			if((correctedState = simulator.getResult()) != null){
				Eug.GetWorld().snapToSnapshot(correctedState);
			}
			
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
		
		Eug.GetWorld().synchronizeSnapshot(snapshot);
		
		EugClient.GetPlayer().setInitialized(true);
		InitializeSceneMessage ack = new InitializeSceneMessage(EugClient.GetPlayer().getInstanceId(), null);
		ack.sendUDP(EugClient.GetPlayer().getConnection());
		
		testEnt = (DiskEntity) Eug.Spawn(new DiskEntity(EugClient.GetInstanceId(), EugClient.GetPlayer()));
		
		Debug.println("[EugClient:initializeScene] Scene initialized and response sent.");
	}
	
	protected void serverUpdate(UpdateMessage msg) {
		if(!EugClient.GetPlayer().isValid()) return;
		
		Snapshot serverSnapshot = msg.getSnapshot();
		latestServerSnapshot = serverSnapshot;
		
		latency = msg.getTravelTime();

		Eug.GetWorld().synchronizeSnapshot(serverSnapshot);
		
		//Since synchronizeSnapshot only accounts for the creation/deletion of new/old players & entities, we need to sync the state of players and entities ourselves.
		for(PlayerState plState : serverSnapshot.getPlayers()){
			Player pl = Eug.FindPlayerById(plState.getId());
			
			if(!pl.getState().equals(plState)){
				pl.snapToState(plState);
			}
		}
		
		//This gets rid of all saved snapshots from before the latest server snapshot.
		while(Eug.GetWorld().getHistory().peek() != null && Eug.GetWorld().getHistory().peek().getTimestamp() < serverSnapshot.getTimestamp()){
			if(Eug.GetWorld().getHistory().count() == 1) break;
			Eug.GetWorld().getHistory().pop();
		}
		
		System.out.println("Client ent count: " + Eug.GetEntities().size());
		if (true) return;
		if(simulator.isRunning()) return; //Don't update while previous correction is still calculating
		
		Snapshot simulatedState = null;
		Snapshot localSnapshot = Eug.GetWorld().getHistory().peek(); 
		
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
			System.out.println("Getting result: " + serverSnapshot);
		}
		
		if(!Snapshot.Compare(serverSnapshot, localSnapshot)){	//If the serverSnapshot and the localSnapshot don't match, calculate a corrected state
			System.out.println("Client/Server out of sync, simulating correction...");
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
