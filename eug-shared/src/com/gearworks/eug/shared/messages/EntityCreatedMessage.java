package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.AbstractEntityState;

/*
 * Sent by the server to notify clients of an entity being spawned
 */
public class EntityCreatedMessage extends Message{
	AbstractEntityState entityState;
	
	public EntityCreatedMessage(){
		entityState = null;
	}
	
	public EntityCreatedMessage(AbstractEntityState es){
		entityState = es;
	}
	
	public AbstractEntityState getEntityState(){ return entityState; }
}
