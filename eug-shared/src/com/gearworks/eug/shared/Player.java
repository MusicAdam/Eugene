package com.gearworks.eug.shared;

import java.util.ArrayList;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.input.PlayerInput;

public class Player {		
	private int 	instanceId;			//Reference to instance this player belongs to 
	private int 	id; 			//id associated with player's connection to the server
	private long	validationTimestamp; 	//Last time AssignInstanceMessage was sent
	private long	lastSnapshotTimestamp; //The last time the player received as snapshot clientside or was sent a snapshot serverside (not included in player state as it is different client/server side
	private boolean isInitialized = false;			//True when the initial snapshot has been successfully sent to the player
	private boolean isDisconnected = false; //When true player will be removed from idle players/instances
	private boolean isInstanceValidated = false; //True after AssignInstance handshake
	private transient ArrayList<NetworkedEntity> entities;
	private transient ArrayList<PlayerInput> inputs; //A record of inputs. On client: inputs will be removed once they have been corrected.
	
	public Player(int id){
		instanceId = -1;
		this.id = id;
		entities = new ArrayList<NetworkedEntity>();
		inputs = new ArrayList<PlayerInput>();
	}
	
	public Player(PlayerState state){
		this.instanceId = state.getInstanceId();
		this.id = state.getId();
		this.validationTimestamp = state.getValidationTimestamp();
		this.isInitialized = state.isInitialized();
		this.isDisconnected = state.isDisconnected();
		this.isInstanceValidated = state.isInstanceValidated();
		entities = new ArrayList<NetworkedEntity>();
		inputs = new ArrayList<PlayerInput>();
	}
	
	
	//Returns true after scene has been initialzied
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public void setInitialized(boolean flag){
		isInitialized = flag;
		
		Debug.println("[Player:setInitialized] [" + id + "] " + flag);
	}
	
	public boolean isDisconnected(){
		return isDisconnected;
	}
	
	public void setDisconnected(boolean flag){
		isDisconnected = flag;
		
		Debug.println("[Player:setDisconnected] [" + id + "] " + flag);
	}
	
	public boolean isConnected(){
		return (getConnection() != null);
	}
	
	public int getInstanceId(){ return instanceId; }
	public void setInstanceId(int id){ 
		instanceId = id;
		Debug.println("[Player:setInstanceId] [" + id + "] " + id);
	}
	public Connection getConnection(){ 
		return Eug.GetConnectionById(id);
	}
	public int getId(){ 
		return id;
	}
	
	//Returns true after client is synced and ready to play
	public boolean isValid(){ return isInstanceValid() && isInitialized() && !isDisconnected(); }
	public boolean isInstanceValid(){ return isInstanceValidated; }
	public void setInstanceValid(boolean toggle){ 
		isInstanceValidated = toggle;
		Debug.println("[Player:setInstanceValid] [" + id + "] " + toggle);
	}
	public long getValidationTimestamp(){ return validationTimestamp; }
	public void setValidationTimestamp(long ts){ validationTimestamp = ts; } 
	public void dispose(){
		for(int i = 0; i < entities.size(); i++){
			Eug.DestroyEntity(entities.get(i));
		}
	}
	
	public void addEntity(NetworkedEntity e){		
		if(!isValid()) return;
		
		if(e.getPlayer() != null && e.getPlayer() != this){
			e.getPlayer().removeEntity(e);
		}
		
		e.setPlayer(this);
		
		entities.add(e);
		
		Debug.println("[Player:addEntity] [" + id + "] added entity " + e.getId());
	}
	
	public void removeEntity(NetworkedEntity e){
		if(entities.remove(e)){
			e.setPlayer(null);
		}
	}
	
	public ArrayList<PlayerInput> getInputs(){ return inputs; }
	public void clearInputs(){ inputs.clear(); }


	public ArrayList<NetworkedEntity> getEntities() {
		return entities;
	}
	
	public PlayerState getState(){
		int[] entityIds = new int[entities.size()];
		for(int i = 0; i < entityIds.length; i++){
			entityIds[i] = entities.get(i).getId();
		}
		
		return new PlayerState(	instanceId,
								id,
								validationTimestamp,
								isInitialized,
								isDisconnected,
								isInstanceValidated);
	}
	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof Player)) return false;
		Player otherPlayer = (Player)other;
		
		return otherPlayer.id == id;
	}

	public void snapToState(PlayerState plState) {
		id = plState.getId();
		if(getInstanceId() != plState.getInstanceId())
			setInstanceId(plState.getInstanceId());
		if(isInitialized() != plState.isInitialized())
			setInitialized(plState.isInitialized());
		if(isDisconnected() != plState.isDisconnected())
			setDisconnected(plState.isDisconnected());
		if(getValidationTimestamp() != plState.getValidationTimestamp())
			setValidationTimestamp(plState.getValidationTimestamp());
	}
	
	public void setLastSnapshotTimestamp(long ts){ lastSnapshotTimestamp = ts; }
	public long getLastSnapshotTimestamp(){ return lastSnapshotTimestamp; }
	
	public void addInput(PlayerInput input) {
		inputs.add(input);
	}
	
	public void resolveInput(PlayerInput input){
		addInput(input);
		
		//TODO: INputMapper		
	}
}
