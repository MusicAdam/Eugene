package com.gearworks.eug.server;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.messages.InputSnapshot;

public class ServerPlayer extends Player {
	
	private InputSnapshot lastInput; //A reference to the player's last input for reference in the next snapshot
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
	
	public void setInputSnapshot(InputSnapshot input){ lastInput = input; }
	public InputSnapshot getInputSnapshot(){ return lastInput; }
}
