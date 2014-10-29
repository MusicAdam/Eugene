package com.gearworks.eug.server;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.input.ClientInput;

public class ServerPlayer extends Player {
	
	private ClientInput lastInput; //A reference to the player's last input for reference in the next snapshot
	private DiskEntity disk;

	public ServerPlayer(int id) {
		super(id);
	}

	
	public void setDisk(DiskEntity disk){
		this.disk = disk;
	}
	
	public DiskEntity getDisk(){
		return disk;
	}
	
	public void setInputSnapshot(ClientInput input){ lastInput = input; }
	public ClientInput getInputSnapshot(){ return lastInput; }
}
