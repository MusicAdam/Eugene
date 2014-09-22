package com.gearworks.eug.shared;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.entities.DiskEntity;

/*
 * TODO: Make player class serializable so that it can be sent in messages
 * 			*No longer have a reference to connection
 * 			*No longer have a diskentity
 * 			*These could both be changes to their respective id's. Tradeoff being you now have to query for connection & entity
 * 				as opposed to just the player. On the ohter hand you no longer have to reconstruct the player on clientside..
 */
public class Player {		
	private int 	instanceId;			//Reference to instance this player belongs to 
	private int 	id; 			//id associated with player's connection to the server
	private long	validationTimestamp; 	//Last time AssignInstanceMessage was sent
	private boolean isInitialized = false;			//True when the initial snapshot has been successfully sent to the player
	private Array<Entity> entities;
	private boolean isDisconnected = false; //When true player will be removed from idle players/instances
	
	public Player(int id){
		instanceId = -1;
		this.id = id;
		entities = new Array<Entity>();
	}
	
	
	//Returns true after scene has been initialzied
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public void setInitialized(boolean flag){
		isInitialized = flag;
	}
	
	public boolean isDisconnected(){
		return isDisconnected;
	}
	
	public void setDisconnected(boolean flag){
		isDisconnected = flag;
	}
	
	public boolean isConnected(){
		return (getConnection() != null);
	}
	
	public int getInstanceId(){ return instanceId; }
	public void setInstanceId(int id){ instanceId = id; }
	public Connection getConnection(){ 
		return Eug.GetConnectionById(id);
	}
	public int getId(){ 
		return id;
	}
	
	//Returns true after client is synced and ready to play
	public boolean isValid(){ return isInstanceValid() && isInitialized() && !isDisconnected(); }
	public boolean isInstanceValid(){ return instanceId != -1; }
	public long getValidationTimestamp(){ return validationTimestamp; }
	public void setValidationTimestamp(long ts){ validationTimestamp = ts; } 
	public void dispose(){
		for(int i = 0; i < entities.size; i++){
			Eug.Destroy(entities.get(i));
		}
	}
	
	public void addEntity(Entity e){
		e.setPlayer(this);
		entities.add(e);
	}
	
	public void removeEntity(Entity e){
		if(entities.removeValue(e, true)){
			e.setPlayer(null);
		}
	}
}
