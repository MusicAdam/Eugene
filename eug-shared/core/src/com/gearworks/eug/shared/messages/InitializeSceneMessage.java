package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.Snapshot;

public class InitializeSceneMessage extends Message {

	int instanceId;
	Snapshot snapshot;
	
	public InitializeSceneMessage(){
		snapshot = null;
	}
	
	public InitializeSceneMessage(int instanceId, Snapshot s){
		snapshot = s;
	}
	
	public Snapshot getSnapshot(){ return snapshot; }
	public int getInstanceId(){ return instanceId; }
}
