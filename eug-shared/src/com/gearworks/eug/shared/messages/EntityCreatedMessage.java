package com.gearworks.eug.shared.messages;

import com.gearworks.eug.shared.state.NetworkedEntityState;

/*
 * Sent by the server to notify clients of an entity being spawned
 */
public class EntityCreatedMessage extends Message{
	NetworkedEntityState entityState;
	
	public EntityCreatedMessage(){
		entityState = null;
	}
	
	public EntityCreatedMessage(NetworkedEntityState es){
		entityState = es;
	}
	
	public NetworkedEntityState getEntityState(){ return entityState; }
}
