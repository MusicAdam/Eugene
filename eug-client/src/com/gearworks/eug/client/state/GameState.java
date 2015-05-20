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

		final GameState thisRef = this; 
		serverUpdateMessageIndex = 	
				EugClient.GetMessageRegistry().listen(UpdateMessage.class, new MessageCallback(){
					@Override
					public void messageReceived(Connection c, Message msg){
						thisRef.serverUpdate((UpdateMessage)msg);
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
		
		if(serverUpdateMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(serverUpdateMessageIndex);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void update() {
		frameStart = Utils.generateTimeStamp();
		
		Eug.GetWorld().update(SharedVars.STEP);
		
		frameTimeSum += (Utils.generateTimeStamp() - frameStart);
	}
	
	protected void serverUpdate(UpdateMessage msg) {
		Snapshot serverSnapshot = msg.getSnapshot();
		
		if(serverSnapshot == null){
			System.out.println("Snapshot null");
			return;
		}
		
		Eug.GetWorld().snapToSnapshot(serverSnapshot);
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
