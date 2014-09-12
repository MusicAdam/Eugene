package com.gearworks.eug.client;

import com.gearworks.eug.client.entities.Disk;

public class ClientPlayer {
	private Disk 	disk;
	
	public void spawnDisk(){
		//Destroy old disk if it exists
		if(disk != null)
			Eug.Destroy(disk);
		disk = (Disk)Eug.Spawn(new Disk(this));
	}
	
	public Disk disk(){ return disk; }
}
