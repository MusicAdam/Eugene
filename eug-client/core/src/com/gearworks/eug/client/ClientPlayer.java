package com.gearworks.eug.client;

import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.entities.DiskEntity;

public class ClientPlayer extends Player {
	
	private DiskEntity disk;
	
	public ClientPlayer(int id) {
		super(id);
	}
	
	public void setDisk(DiskEntity disk){
		this.disk = disk;
	}
	
	public DiskEntity getDisk(){
		return disk;
	}
}
