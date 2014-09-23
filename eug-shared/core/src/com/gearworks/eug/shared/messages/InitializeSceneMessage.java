package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.ServerState;

public class InitializeSceneMessage extends Message {

	int instanceId;
	ServerState state;
	
	public InitializeSceneMessage(){
		state = null;
	}
	
	public InitializeSceneMessage(int instanceId, ServerState s){
		state = s;
	}
	
	public ServerState getState(){ return state; }
	public int getInstanceId(){ return instanceId; }
}
