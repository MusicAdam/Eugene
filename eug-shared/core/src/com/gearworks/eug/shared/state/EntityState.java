package com.gearworks.eug.shared.state;

import com.badlogic.gdx.physics.box2d.Transform;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Serializable class that represents the state of an entity.
 */
public class EntityState {	
	private long timestamp;			//The time at which the state was generated
	private int id;					//Id of the entity
	private int playerId;			//Id of the player
	private Entity.Type type;				//Type of the entity
	private String spriteResource;	//Name of the texture for the sprite. null if none exists
	private BodyState bodyState; //The state of the body in the physical world. NUll if none exists
	
	public static EntityState GenerateTestState(Entity ent){
		EntityState state = new EntityState();
		state.id = ent.getId();
		state.playerId = ent.getPlayer().getId();
		state.bodyState = BodyState.GenerateTestState(ent);
		state.spriteResource = ent.getSpriteResource();
		state.type = ent.getType();
		return state;
	}
	
	public EntityState(){
		timestamp = Utils.generateTimeStamp();
		id = -1;
		playerId = -1;
		bodyState = null;
		spriteResource = null;
	}	
	
	public EntityState(Entity ent) {
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
	}
	
	public EntityState(EntityState cpy){
		this.timestamp = cpy.timestamp;
		this.id = cpy.id;
		this.playerId = cpy.playerId;		
		this.bodyState = new BodyState(cpy.bodyState);		
		this.spriteResource = cpy.spriteResource;
		this.type = cpy.type;
	}


	public long getTimestamp() { return timestamp; }
	public int getId() { return id; }
	public int getPlayerId() {	return playerId;	}
	public BodyState getBodyState() {		return bodyState;	}
	public String getSpriteResource() {		return spriteResource;	}
	public Entity.Type getType(){ return type; }

	public static boolean Compare(EntityState correctedState, EntityState entState) {
		if(correctedState == null || entState == null)
			return false;
		
		float distance = correctedState.getBodyState().getTransform().getPosition().cpy().sub(entState.getBodyState().getTransform().getPosition()).len(); 
		float angle = Math.abs(correctedState.getBodyState().getTransform().getRotation() - entState.getBodyState().getTransform().getRotation());
		//System.out.println(correctedState.id + ":" + entState.id);
		//System.out.println("\tPos: " + correctedState.getBodyState().getTransform().getPosition() + ", " + entState.getBodyState().getTransform().getPosition());
		//System.out.println("\tDist: " + (distance <= SharedVars.POSITION_TOLERANCE));
		//System.out.println("\tAngle: " + (angle <= SharedVars.ROTATION_TOLERANCE));
		return (distance <= SharedVars.POSITION_TOLERANCE) && (angle <= SharedVars.ROTATION_TOLERANCE);
	}
	
}
