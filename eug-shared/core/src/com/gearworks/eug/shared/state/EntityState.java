package com.gearworks.eug.shared.state;

import com.badlogic.gdx.physics.box2d.Transform;
import com.gearworks.eug.shared.Utils;

/*
 * Serializable class that represents the state of an entity.
 */
public class EntityState {
	private long timestamp;			//The time at which the state was generated
	private int id;					//Id of the entity
	private int playerId;			//Id of the player
	private String spriteResource;	//Name of the texture for the sprite. null if none exists
	private BodyState bodyState; //The state of the body in the physical world. NUll if none exists
	
	public EntityState(){
		timestamp = Utils.generateTimeStamp();
		id = -1;
		playerId = -1;
		bodyState = null;
		spriteResource = null;
	}	
	
	public EntityState(int id, int playerId,
			BodyState bodyState, String spriteResource) {
		this.timestamp = Utils.generateTimeStamp();
		this.id = id;
		this.playerId = playerId;
		this.bodyState = bodyState;
		this.spriteResource = spriteResource;
	}



	public long getTimestamp() { return timestamp; }
	public int getId() { return id; }
	public int getPlayerId() {	return playerId;	}
	public BodyState getBodyState() {		return bodyState;	}
	public String getSpriteResource() {		return spriteResource;	}
}
