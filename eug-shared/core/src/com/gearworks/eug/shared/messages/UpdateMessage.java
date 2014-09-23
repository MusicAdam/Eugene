package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.ServerState;

/*
 * Sent by the server to update client positions
 */
public class UpdateMessage extends Message {
	ServerState state;
	
	public UpdateMessage(){
		state = null;
	}
	
	public UpdateMessage(ServerState s){
		state = s;
	}
	
	public ServerState getState(){ return state; }
}
