package com.gearworks.eug.shared.state;

import java.util.ArrayList;

import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.PlayerState;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.utils.CircularBuffer;
import com.gearworks.eug.shared.utils.Utils;

/*
 * A snapshot contains all the information related to the state of game entities at the time given.
 */
public class Snapshot {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp;
	private AbstractEntityState[] entityStates; //The states for all the entities in the server
	private PlayerState[] players; //Players who are connected	
	private PlayerInput[] inputs; //Record of inputs since last snapshot
	private int tick;
	
	public Snapshot(){
		timestamp = Utils.generateTimeStamp();
		instanceId = -1;
		entityStates = null;
		tick = -1;
	}

	public Snapshot(int instanceId, PlayerState[] players, AbstractEntityState[] entityStates, PlayerInput[] playerInputs, int tick) {
		timestamp = Utils.generateTimeStamp();
		this.instanceId = instanceId;
		this.entityStates = entityStates;
		this.players = players;
		this.inputs = playerInputs;
		this.tick = tick;
		
		for(PlayerInput input : playerInputs){
			input.setSnapshot(this);
		}
	}
	
	public Snapshot(Snapshot cpy){
		this.timestamp = cpy.timestamp;
		this.instanceId = cpy.instanceId;
		this.tick = cpy.tick;
		
		if(cpy.entityStates != null){
			this.entityStates = new AbstractEntityState[cpy.entityStates.length];
			for(int i = 0; i < cpy.entityStates.length; i++){
				this.entityStates[i] = cpy.entityStates[i].clone();
			}
		}
		
		if(cpy.players != null){
			this.players = new PlayerState[cpy.players.length];
			for(int i = 0; i < cpy.players.length; i++){
				this.players[i] = new PlayerState(cpy.players[i]);
			}
		}
		
		if(cpy.inputs != null){
			this.inputs = new PlayerInput[cpy.inputs.length];
			for(int i = 0; i < inputs.length; i++){
				this.inputs[i] = new PlayerInput(cpy.inputs[i]);
			}
		}
	}
	
	public int getTick(){ return tick; }
	public int getInstanceId() { return instanceId;	}
	public long getTimestamp(){ return timestamp; }
	public AbstractEntityState[] getEntityStates() { return entityStates; }

	/*
	 * NOTE: Efficiency can be increased by created a hashmap while iterating entityStates to associate id's with indices.
	 * 		probably only worth it if we are expecting to have a lot of entities.
	 */
	public AbstractEntityState getEntityState(int id) {
		if(entityStates == null) return null;
		for(int i = 0; i < entityStates.length; i++){
			if(entityStates[i] == null) continue;
			if(entityStates[i].getId() == id)
				return entityStates[i];
		}

		return null;
	}

	public static boolean Compare(Snapshot serverSnapshot, Snapshot simulatedState) {	
		if(serverSnapshot == null || simulatedState == null) return false;
		
		for(int i = 0; i < serverSnapshot.entityStates.length; i++){
			if(!serverSnapshot.entityStates[i].epsilonEquals(simulatedState.getEntityState(serverSnapshot.entityStates[i].getId())))
				return false;
		}
		return true;
	}

	public void setTimestamp(long time) {
		timestamp = time;
	}

	public PlayerState[] getPlayers() {
		return players;
	}
	
	public PlayerInput[] getInput(){
		return inputs;
	}
	
	public void addInput(PlayerInput input){
		if(input.isSaved()){
			//return; //If this input is already in another snapshot, don't add it to this one.
		}
		
		//Create a new input array with  the snapshots current inputs + 1 for the new input
		int fixedSize = getInput().length + 1;
		PlayerInput[] inputs = new PlayerInput[fixedSize];
		
		//Copy over old inputs
		for(int i = 0; i < getInput().length; i++){
			inputs[i] = getInput()[i];
		}
		
		inputs[fixedSize - 1] = input;
		
		input.setSnapshot(this);
		
		this.inputs = inputs;
	}
}
