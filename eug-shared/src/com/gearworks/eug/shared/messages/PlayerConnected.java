package com.gearworks.eug.shared.messages;

//Notify that another player has connected
public class PlayerConnected extends Message {
	private int instanceId;
	private int playerId;
	
	public PlayerConnected(){}
	public PlayerConnected(int instanceId, int playerId){
		this.instanceId = instanceId;
		this.playerId = playerId;
	}
	public int getInstanceId() {
		return instanceId;
	}
	public int getPlayerId() {
		return playerId;
	}

}
