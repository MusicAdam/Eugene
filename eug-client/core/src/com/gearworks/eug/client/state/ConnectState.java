package com.gearworks.eug.client.state;

import java.io.IOException;

import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.state.State;

/*
 * Connect state is responsible for initializing the client and connecting it to a server
 * 
 * For now, it tries to connect to a fixed IP
 */
public class ConnectState implements State {
	private boolean done = false;
	private int attempts = 0;
	private int maxAttempts = 5;

	@Override
	public void render() {
	}

	@Override
	public void update() {
		if(attempts < maxAttempts && !EugClient.GetClient().isConnected()){			
			//Initialize client
			try {
				Debug.print("\t");
				EugClient.GetClient().connect(5000, "localhost", SharedVars.TCP_PORT, SharedVars.UDP_PORT);
			} catch (IOException e) {
				Debug.print("\t");
				Debug.println(e.getMessage());
			}
			
			attempts++;
		}else{			
			done = true;
		}
		
		EugClient.GetStateManager().setState(new GameState());
	}

	@Override
	public void onEnter() {		
		Debug.println("[ConnectState:onEnter] Attempting to connect to localhost on " + SharedVars.TCP_PORT + ", " + SharedVars.UDP_PORT);
	}

	@Override
	public void onExit() {
		if(EugClient.GetClient().isConnected()){
			Debug.println("[ConnectState:onExit] Connection established.");
		}else{
			Debug.println("[ConnectState:onExit] Unable to establish connection to server.");
		}
	}

	@Override
	public boolean canEnterState() {
		return true;
	}

	@Override
	public boolean canExitState() {
		return done;
	}

	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}

}
