package com.gearworks.eug.shared.state;

import com.gearworks.eug.shared.utils.Utils;

/*
 * A snapshot contains all the information related to the state of game entities at the time given.
 */
public class Snapshot {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp;
	private int tick; //Keeps track of the client tick to which this 
	private EntityState[] entityStates; //The states for all the entitites in this instance
	
	public Snapshot(){
		timestamp = Utils.generateTimeStamp();
		tick = -1;
		instanceId = -1;
		entityStates = null;
	}

	public Snapshot(int instanceId, EntityState[] entityStates) {
		timestamp = Utils.generateTimeStamp();
		this.instanceId = instanceId;
		this.entityStates = entityStates;
	}
	
	public int getInstanceId() { return instanceId;	}
	public int getTick() { return tick; }
	public void setTick(int tick){ this.tick = tick; }
	public long getTimestamp(){ return timestamp; }
	public EntityState[] getEntityStates() { return entityStates; }

	/*
	 * NOTE: Efficiency can be increased by created a hashmap while iterating entityStates to associate id's with indices.
	 * 		probably only worth it if we are expecting to have a lot of entities.
	 */
	public EntityState getEntityState(int id) {
		for(int i = 0; i < entityStates.length; i++){
			if(entityStates[i].getId() == id)
				return entityStates[i];
		}
		return null;
	}
}
