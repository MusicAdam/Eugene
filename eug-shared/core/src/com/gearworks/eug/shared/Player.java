package com.gearworks.eug.shared;

public class Player {		
	private int 	instanceId;			//Unique instace id assigned by the server once a connection has been made
	
	public Player(){
		instanceId = -1;
	}
	
	
	public int instanceId(){ return instanceId; }
	public void instanceId(int id){ instanceId = id; }
}
