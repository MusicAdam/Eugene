package com.gearworks.eug.shared;

import java.util.ArrayList;

import com.gearworks.eug.shared.input.PlayerInput;


//Encapsulates the state of a player so that it may be sent over the network
//A players state does not include entities it owns
public class PlayerState {
	private int 	instanceId;			//Reference to instance this player belongs to 
	private int 	id; 			//id associated with player's connection to the server
	private boolean isDisconnected = false; //When true player will be removed from idle players/instances
	
	public PlayerState(){
		//For Serialization
	}
	
	public PlayerState(int instanceId, int id, boolean isDisconnected){
		this.instanceId = instanceId;
		this.id = id;
		this.isDisconnected = isDisconnected;
	}
	
	public PlayerState(PlayerState cpy){
		this.instanceId = cpy.instanceId;
		this.id = cpy.id;
		this.isDisconnected = cpy.isDisconnected;
	}
	
	public int getInstanceId() {
		return instanceId;
	}

	public int getId() {
		return id;
	}
	
	public boolean isDisconnected() {
		return isDisconnected;
	}

	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof PlayerState)) return false;
		if(other == this) return true;
		
		PlayerState otherPlayer = (PlayerState)other;
		
		return (id == otherPlayer.id &&
				instanceId == otherPlayer.instanceId &&
				isDisconnected == otherPlayer.isDisconnected );
	}
	
	@Override
	public int hashCode(){
		int hash = 5;
		hash = hash * 31 + id;
		hash = hash * 31 + instanceId;
		hash = hash * 31 + ((isDisconnected) ? 1 : 0);

		return hash;
	}
}
