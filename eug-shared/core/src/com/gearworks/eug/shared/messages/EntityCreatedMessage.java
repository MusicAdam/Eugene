package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.EntityState;

/*
 * Sent by the server to notify clients of an entity being spawned
 */
public class EntityCreatedMessage extends Message{
	EntityState entityState;
	
	public EntityCreatedMessage(){
		entityState = null;
	}
	
	public EntityCreatedMessage(EntityState es){
		entityState = es;
	}
	
	public EntityState getEntityState(){ return entityState; }
}
