package com.gearworks.eug.shared.state;

import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Serializable class that represents the state of an entity.
 */
public class NetworkedEntityState implements Cloneable{	
	protected long timestamp;			//The time at which the state was generated
	protected short id;					//Id of the entity
	protected int playerId;				//Id of the player
	protected short type;				//Type of the entity
	
	public NetworkedEntityState(){
		timestamp = Utils.generateTimeStamp();
		id = -1;
		playerId = -1;
	}	
	
	public NetworkedEntityState(NetworkedEntity ent) {		
		this.timestamp = Utils.generateTimeStamp();
		this.id = ent.getId();
		this.playerId = (ent.getOwner() == null) ? -1 : ent.getOwner().getId();		
		this.type = ent.getType();	
	}


	public long getTimestamp() { return timestamp; }
	public short getId() { return id; }
	public int getPlayerId() {	return playerId;	}
	public short getType(){ return type; }
	public void setTimeStamp(long ts){ timestamp = ts; }
	public void setId(short id){ this.id= id; }
	public void setPlayerId(Integer playerId){ this.playerId = playerId; }
	public void setType(short type){ this.type = type; }
	
	
	/** Indicates whether this state is approximately equal to another state. All fields except timestamp are checked
	 * as a state may be equal to a state from another time.
	 * @param other entity to be checked
	 */
	public boolean epsilonEquals(NetworkedEntityState other){
		return (id == other.id &&
				playerId == other.playerId &&
				type == other.type);
				
	}
	
	protected Object clone() throws CloneNotSupportedException{
		NetworkedEntity clone = (NetworkedEntity)super.clone();
		return clone;
	}
}
