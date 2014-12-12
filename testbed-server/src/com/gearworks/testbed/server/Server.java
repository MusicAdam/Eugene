package com.gearworks.testbed.server;

import com.gearworks.eug.server.EugServer;
import com.gearworks.eug.server.Instance;
import com.gearworks.eug.shared.EntityManager;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.events.PlayerEventListener;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityNotRegisteredException;
import com.gearworks.shared.Initializer;
import com.gearworks.testbed.shared.entities.Entity;

public class Server {
	EugServer server;
	Entity testEntity;
	
	public void start(){
		try{
			init();
			while(server.isRunning()){
				server.update(SharedVars.STEP);
			}
		}catch (Exception e){
			throw e;
		}finally{
			server.dispose();
		}
	}
	
	private void init(){
		
		server = (EugServer) Eug.Set(new EugServer());
		Initializer.Initialize();
		server.create();
		
		Entity.RegisterEntities();
		
		Eug.AddPlayerListener(new PlayerEventListener(){
			@Override
			public void Validated(Player player){
				Instance playerInstance = EugServer.GetInstanceByID(player.getInstanceId());
				if(playerInstance == null){
					System.out.println("Instance " + player.getInstanceId() + " not found");
					return;
				}
				
				try {
					EntityManager.Build(player, Entity.ENTITY, playerInstance.getWorld());
				} catch (EntityNotRegisteredException e) {
					e.printStackTrace();
				} catch (EntityBuildException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public static void main(String[] args){
		new Server().start();
	}
}
