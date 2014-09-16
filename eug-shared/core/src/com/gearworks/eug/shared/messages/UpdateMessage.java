package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.Snapshot;

/*
 * Sent by the server to update client positions
 */
public class UpdateMessage extends Message {
	Snapshot snapshot;
	
	public UpdateMessage(){
		snapshot = null;
	}
	
	public UpdateMessage(Snapshot s){
		snapshot = s;
	}
	
	public Snapshot getSnapshot(){ return snapshot; }
}
