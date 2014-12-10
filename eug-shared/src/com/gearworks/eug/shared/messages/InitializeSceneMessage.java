package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.Snapshot;


public class InitializeSceneMessage extends Message {

	int instanceId;
	Snapshot snap;
	
	public InitializeSceneMessage(){
		snap = null;
	}
	
	public InitializeSceneMessage(int instanceId, Snapshot s){
		snap = s;
	}
	
	public Snapshot getSnapshot(){ return snap; }
	public int getInstanceId(){ return instanceId; }
}
