package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.Snapshot;

/*
 * Sent by the server to update client with the latest snapshot
 */
public class UpdateMessage extends Message {
	Snapshot state;
	
	public UpdateMessage(){
		state = null;
	}
	
	public UpdateMessage(Snapshot s){
		state = s;
	}
	
	public Snapshot getSnapshot(){ return state; }
}
