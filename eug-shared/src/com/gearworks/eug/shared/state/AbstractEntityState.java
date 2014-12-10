package com.gearworks.eug.shared.state;

import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Serializable class that represents the state of an entity.
 */
public abstract class AbstractEntityState {	
	private long timestamp;			//The time at which the state was generated
	private int id;					//Id of the entity
	private int playerId;			//Id of the player
	private NetworkedEntity.Type type;		//Type of the entity
	private String spriteResource;	//Name of the texture for the sprite. null if none exists
	
	public AbstractEntityState(){
		timestamp = Utils.generateTimeStamp();
		id = -1;
		playerId = -1;
		spriteResource = null;
	}	
	
	public AbstractEntityState(NetworkedEntity ent) {		
		fromEntity(ent);
	}


	public long getTimestamp() { return timestamp; }
	public int getId() { return id; }
	public int getPlayerId() {	return playerId;	}
	public String getSpriteResource() {		return spriteResource;	}
	public NetworkedEntity.Type getType(){ return type; }
	
	public abstract AbstractEntityState clone();
	public abstract boolean epsilonEquals(AbstractEntityState other);
	public void fromEntity(NetworkedEntity ent){
		this.timestamp = Utils.generateTimeStamp();
		this.id = ent.getId();
		this.playerId = ent.getPlayer().getId();		
		this.spriteResource = ent.getSpriteResource();
		this.type = ent.getType();		
	}
}
