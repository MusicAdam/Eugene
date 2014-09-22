package com.gearworks.eug.server;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.entities.DiskEntity;

public class ServerPlayer extends Player {
	
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
}
