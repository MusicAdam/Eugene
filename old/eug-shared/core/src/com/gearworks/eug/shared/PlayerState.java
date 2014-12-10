package com.gearworks.eug.shared;

import java.util.ArrayList;

import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.input.PlayerInput;


//Encapsulates the state of a player so that it may be sent over the network
//A players state does not include entities it owns
public class PlayerState {
	private int 	instanceId;			//Reference to instance this player belongs to 
	private int 	id; 			//id associated with player's connection to the server
	private long	validationTimestamp; 	//Last time AssignInstanceMessage was sent
	private boolean isInitialized = false;			//True when the initial snapshot has been successfully sent to the player
	private boolean isDisconnected = false; //When true player will be removed from idle players/instances
	private int 	myDiskId; 
	
	public PlayerState(){
		//For Serialization
	}
	
	public PlayerState(int instanceId, int id, long validationTimestamp, boolean isInitialized, boolean isDisconnected, int myDiskId){
		this.instanceId = instanceId;
		this.id = id;
		this.validationTimestamp = validationTimestamp;
		this.isInitialized = isInitialized;
		this.isDisconnected = isDisconnected;
		this.myDiskId = myDiskId;
	}
	
	public PlayerState(PlayerState cpy){
		this.instanceId = cpy.instanceId;
		this.id = cpy.id;
		this.validationTimestamp = cpy.validationTimestamp;
		this.isInitialized = cpy.isInitialized;
		this.isDisconnected = cpy.isDisconnected;
		this.myDiskId = cpy.myDiskId;
	}
	
	public int getInstanceId() {
		return instanceId;
	}

	public int getId() {
		return id;
	}

	public long getValidationTimestamp() {
		return validationTimestamp;
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	public boolean isDisconnected() {
		return isDisconnected;
	}

	public int getMyDiskId() {
		return myDiskId;
	}
	

	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof PlayerState)) return false;
		if(other == this) return true;
		
		PlayerState otherPlayer = (PlayerState)other;
		
		return (id == otherPlayer.id &&
				instanceId == otherPlayer.instanceId &&
				validationTimestamp == otherPlayer.validationTimestamp &&
				isInitialized == otherPlayer.isInitialized &&
				isDisconnected == otherPlayer.isDisconnected &&
				myDiskId == otherPlayer.myDiskId);
	}
	
	@Override
	public int hashCode(){
		int hash = 5;
		hash = hash * 31 + id;
		hash = hash * 31 + instanceId;
		hash = hash * 31 + ((isInitialized) ? 1 : 0);
		hash = hash * 31 + ((isDisconnected) ? 1 : 0);
		hash = hash * 31 + myDiskId;

		return hash;
	}
}
