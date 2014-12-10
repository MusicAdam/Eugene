package com.gearworks.eug.shared.messages;

public class EntityDestroyedMessage extends Message{
	private int entityId;
	
	public EntityDestroyedMessage(){
		entityId = -1;
	}
	
	public EntityDestroyedMessage(int entityId){
		this.entityId = entityId;
	}
	
	public int getEntityId(){ return entityId; }
}
