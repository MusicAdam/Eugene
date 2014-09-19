package com.gearworks.eug.shared.state;

import com.gearworks.eug.shared.Utils;

/*
 * A snapshot contains all the information related to the state of the server at the time given.
 */
public class Snapshot {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp; //Time the server created the snapshot.
	private EntityState[] entityStates; //The states for all the entitites in this instance
	private int[] playerIds; //List of players id's who are connected to this instnace.
	
	public Snapshot(){
		timestamp = Utils.generateTimeStamp();
		instanceId = -1;
		entityStates = null;
	}

	public Snapshot(int instanceId, int[] playerIds, EntityState[] entityStates) {
		this.instanceId = instanceId;
		this.timestamp = Utils.generateTimeStamp();
		this.entityStates = entityStates;
		this.playerIds = playerIds;
	}
	
	public int getInstanceId() { return instanceId;	}
	public long getTimestamp() { return timestamp; }
	public EntityState[] getEntityStates() { return entityStates; }
	public int[] getPlayerIds(){ return playerIds; }
}
