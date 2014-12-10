package com.gearworks.eug.shared.messages;

/*
 * Encapsulates the client/server handshake for instance assignment. 
 * Server sends client instanceId and playerId = -1, client returns 
 * with the playerId and instanceId set, confirming the assignment.
 */
public class AssignInstanceMessage extends Message{
	private int instanceId;
	private int playerId;
	
	public AssignInstanceMessage(){
		instanceId = -1;
		playerId = -1;
	}
	
	public AssignInstanceMessage(int instanceId, int playerId){
		this.instanceId = instanceId;
		this.playerId = playerId;
	}
	
	public int getInstanceId(){ return instanceId; }
	public int getPlayerId(){ return playerId; }
}
