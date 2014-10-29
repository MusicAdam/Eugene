package com.gearworks.eug.shared.state;

import com.gearworks.eug.shared.input.ClientInput;
import com.gearworks.eug.shared.utils.Utils;

/*
 * A server snapshot encapsulates a snapshot but includes information on players who have connected/disconnected
 */
public class ServerState {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp;
	private Snapshot snapshot;
	private ClientInput input; //This input from the client that this snapshot addresses
	private int[] playerIds; //List of players id's who are connected to this instnace.
	private int[] disconnectedPlayers; //List of players who have disconnected since last snapshot
	
	public ServerState(){
		timestamp = Utils.generateTimeStamp();
		instanceId = -1;
		snapshot = null;
	}

	public ServerState(int instanceId, int[] playerIds, int[] disconnectedPlayers, Snapshot snapshot) {
		timestamp = Utils.generateTimeStamp();
		this.instanceId = instanceId;
		this.snapshot = snapshot;
		this.playerIds = playerIds;
		this.disconnectedPlayers = disconnectedPlayers;
	}
	
	public int getInstanceId() { return instanceId;	}
	public long getTimestamp(){ return timestamp; }
	public Snapshot getSnapshot() { return snapshot; }
	public int[] getPlayerIds(){ return playerIds; }
	public int[] getDisconnectedPlayers() {
		return disconnectedPlayers;
	}
	public void setInput(ClientInput input){ this.input = input; }
	public ClientInput getInput(){ return input; }
}
