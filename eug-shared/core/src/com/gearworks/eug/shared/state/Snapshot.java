package com.gearworks.eug.shared.state;

import com.gearworks.eug.shared.Utils;

/*
 * A snapshot contains all the information related to the state of the server at the time given.
 */
public class Snapshot {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp;
	private int tick; //Time the server created the snapshot.
	private EntityState[] entityStates; //The states for all the entitites in this instance
	private int[] playerIds; //List of players id's who are connected to this instnace.
	private int[] disconnectedPlayers; //List of players who have disconnected since last snapshot
	
	public Snapshot(){
		timestamp = Utils.generateTimeStamp();
		tick = -1;
		instanceId = -1;
		entityStates = null;
	}

	public Snapshot(int instanceId, int tick, int[] playerIds, int[] disconnectedPlayers, EntityState[] entityStates) {
		timestamp = Utils.generateTimeStamp();
		this.instanceId = instanceId;
		this.tick = tick;
		this.entityStates = entityStates;
		this.playerIds = playerIds;
		this.disconnectedPlayers = disconnectedPlayers;
	}
	
	public int getInstanceId() { return instanceId;	}
	public int getTick() { return tick; }
	public long getTimestamp(){ return timestamp; }
	public EntityState[] getEntityStates() { return entityStates; }
	public int[] getPlayerIds(){ return playerIds; }
	public int[] getDisconnectedPlayers() {
		return disconnectedPlayers;
	}
}
