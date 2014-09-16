package com.gearworks.eug.shared.state;

import com.gearworks.eug.shared.Utils;

/*
 * A snapshot contains all the information related to the state of the server at the time given.
 */
public class Snapshot {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp; //Time the server created the snapshot.
	private EntityState[] entityStates; //The states for all the entitites in this instance
	
	public Snapshot(){
		timestamp = Utils.generateTimeStamp();
		instanceId = -1;
		entityStates = null;
	}

	public Snapshot(int instanceId, EntityState[] entityStates) {
		this.instanceId = instanceId;
		this.timestamp = Utils.generateTimeStamp();
		this.entityStates = entityStates;
	}
	
	public int getInstanceId() { return instanceId;	}
	public long getTimestamp() { return timestamp; }
	public EntityState[] getEntityStates() { return entityStates; }
}
