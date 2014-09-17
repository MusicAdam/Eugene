package com.gearworks.eug.shared;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.entities.DiskEntity;

public class Player {		
	private int 	instanceId;			//Reference to instance this player belongs to 
	private Connection connection; 			//Connection associated with player;
	private long	validationTimestamp; 	//Last time AssignInstanceMessage was sent
	private boolean isInitialized = false;			//True when the initial snapshot has been successfully sent to the player
	private DiskEntity disk;
	
	public Player(Connection conn){
		instanceId = -1;
		connection = conn;
	}
	
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public void setInitialized(boolean flag){
		isInitialized = flag;
	}
	public int getInstanceId(){ return instanceId; }
	public void setInstanceId(int id){ instanceId = id; }
	public Connection getConnection(){ return connection; }
	public void setConnection(Connection c){ connection = c; }
	public int getId(){ 
		if(getConnection() == null)
			return -1;
		return getConnection().getID();
	}
	public boolean isValid(){ return getId() != -1; }
	public boolean isInstanceValid(){ return instanceId != -1; }
	public long getValidationTimestamp(){ return validationTimestamp; }
	public void setValidationTimestamp(long ts){ validationTimestamp = ts; } 
	public void dispose(){}

	public DiskEntity getDisk() {
		return disk;
	}

	public void setDisk(DiskEntity disk) {
		this.disk = disk;
	}
}
