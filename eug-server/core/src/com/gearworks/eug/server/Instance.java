package com.gearworks.eug.server;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.Utils;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;

public class Instance {
	public static int MAX_PLAYERS = 4;
	public static long VALIDATION_DELAY = 100; //Time in miliseconds to wait before resending instance validaiton
	
	private int id;
	private Array<ServerPlayer> 	players;
	private Array<Entity>	entities;
	private World			world;
	private Queue<ServerPlayer> 	removePlayerQueue;
	
	boolean test = true;
	
	public Instance(int id)
	{
		this.id = id;
		removePlayerQueue = new ConcurrentLinkedQueue<ServerPlayer>();
		players = new Array<ServerPlayer>();
		entities = new Array<Entity>();
		world = new World(SharedVars.GRAVITY, SharedVars.DO_SLEEP);
		
		//Setup message handlers
		final Instance thisInst = this;
		EugServer.GetMessageRegistry().register(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				if(aMsg.getInstanceId() != thisInst.getId()) return;
				if(aMsg.getPlayerId() == -1) return;
				
				//Validate player
				for(Player pl : thisInst.players){
					if(!pl.isInstanceValid() && pl.getId() == aMsg.getPlayerId()){
						pl.setInstanceId(thisInst.getId());
					}
				}
			}
		});
	}
	
	public void update(){
		for(ServerPlayer pl : players){
			//Check for invalid players
			if(!pl.isInstanceValid()){
				//Resend message if enough time has elapsed
				if(Utils.generateTimeStamp() - pl.getValidationTimestamp() >= VALIDATION_DELAY){
					Debug.println("[Instance:update] [" + id + "] sending AssignInstanceMessage to player " + pl.getId() + ".");
					sendAssignInstanceMessage(pl);
				}				
			}else{
				if(test){
					test = false;
					System.out.println(entities.size);
					EugServer.Spawn(new DiskEntity(entities.size, pl));
				}
			}
			
			//Check for disconnected players
			//System.out.println(pl.getConnection());
			if(pl.getConnection() == null){
				removePlayerQueue.add(pl);
			}
		}
		
		ServerPlayer toRemove = null;
		while((toRemove = removePlayerQueue.poll()) != null)
			removePlayer(toRemove);
	}
	
	//Attempts to add player to the instance, returns true on success false if instance if full
	//Additionally sends player a message that he has been added to this instance
	public boolean addPlayer(ServerPlayer player){
		if(isFull()) return false;
		
		players.add(player);
		sendAssignInstanceMessage(player);
		
		Debug.println("[Instance:addPlayer] [" + getId() + "] Added player " + player.getId());
		return true;
	}
	
	//Attempts to remove a player from the instance, return true on success, false if player is not in instance
	public boolean removePlayer(ServerPlayer player){
		if(players.removeValue(player, true)){
			player.setInstanceId(-1);
			EugServer.QueueIdlePlayer(player);
			Debug.println("[Instance:removePlayer] ["+getId()+"] Removed player " + player.getId());
			return true;
		}
		
		return false;
	}
	
	public Entity addEntity(Entity ent){
		entities.add(ent);
		return ent;
	}
	
	public void removeEntity(Entity ent){
		entities.removeValue(ent, true);
	}
	
	public void sendAssignInstanceMessage(ServerPlayer pl){
		AssignInstanceMessage msg = new AssignInstanceMessage(id, -1);
		pl.setValidationTimestamp(Utils.generateTimeStamp());
		pl.getConnection().sendUDP(msg);
	}
	
	public boolean isFull(){ return players.size == MAX_PLAYERS; }
	public World getWorld(){ return world; }	
	public int getId(){ return id; }

	public ServerPlayer findPlayerByConnection(Connection connection) {
		for(ServerPlayer pl : players){
			if(pl.getConnection() == connection)
				return pl;
		}
		
		return null;
	}
}
