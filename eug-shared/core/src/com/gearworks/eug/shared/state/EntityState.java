package com.gearworks.eug.shared.state;

import com.badlogic.gdx.physics.box2d.Transform;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Serializable class that represents the state of an entity.
 */
public class EntityState {
	public static final int CREATE = 1;
	public static final int UPDATE  = 0;
	public static final int DESTROY = -1;
	
	private long timestamp;			//The time at which the state was generated
	private int id;					//Id of the entity
	private int playerId;			//Id of the player
	private Entity.Type type;				//Type of the entity
	private String spriteResource;	//Name of the texture for the sprite. null if none exists
	private BodyState bodyState; //The state of the body in the physical world. NUll if none exists
	private int status;				//-1 = destroyed, 0 = update, 1 = created
	
	public EntityState(){
		timestamp = Utils.generateTimeStamp();
		id = -1;
		playerId = -1;
		bodyState = null;
		spriteResource = null;
	}	
	
	public EntityState(Entity ent, int status) {
		this.timestamp = Utils.generateTimeStamp();
		this.id = ent.getId();
		this.playerId = ent.getPlayer().getId();
		
		this.bodyState = new BodyState();
		if(ent.body() != null){
			BodyState.FromEntity(ent, this.bodyState);
		}else{
			this.bodyState = null;
		}
		
		this.spriteResource = ent.getSpriteResource();
		this.type = ent.getType();
		this.status = status;
	}



	public long getTimestamp() { return timestamp; }
	public int getId() { return id; }
	public int getPlayerId() {	return playerId;	}
	public BodyState getBodyState() {		return bodyState;	}
	public String getSpriteResource() {		return spriteResource;	}
	public Entity.Type getType(){ return type; }
	public boolean wasCreated(){ return status == CREATE; }
	public boolean wasUpdated(){ return status == UPDATE; }
	public boolean wasDestroyed(){ return status == DESTROY; }

	public static boolean Compare(EntityState correctedState, EntityState entState) {
		float distance = correctedState.getBodyState().getTransform().getPosition().cpy().sub(entState.getBodyState().getTransform().getPosition()).len(); 
		float angle = Math.abs(correctedState.getBodyState().getTransform().getRotation() - entState.getBodyState().getTransform().getRotation());
		return (distance <= SharedVars.POSITION_TOLERANCE) && (angle <= SharedVars.ROTATION_TOLERANCE);
	}
	
}
