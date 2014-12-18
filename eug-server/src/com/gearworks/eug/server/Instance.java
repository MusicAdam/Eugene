package com.gearworks.eug.server;

import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.Simulator;
import com.gearworks.eug.shared.World;
import com.gearworks.eug.shared.events.PlayerEventListener;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.input.PlayerInput.Event;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.EntityCreatedMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.AbstractEntityState;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.Utils;

public class Instance {
	public static int MAX_PLAYERS = 4;
	public static long VALIDATION_DELAY = 100; //Time in miliseconds to wait before resending instance validaiton
	public static long SNAPSHOT_DELAY  = 100; //Time in ms to wait before sending new snapshot
	
	private int id;
	private World			world;
	private Queue<ServerPlayer> 	removePlayerQueue;
	private ServerPlayer serverPlayer; //This is the player to which level entities belong.
	private Simulator simulator;

	public Instance(int id)
	{
		this.id = id;
		removePlayerQueue = new ConcurrentLinkedQueue<ServerPlayer>();
		world = new World("InstanceWorld", id);
		serverPlayer = new ServerPlayer(-1);
		serverPlayer.setInitialized(true);
		serverPlayer.setInstanceId(id);
		serverPlayer.setInstanceValid(true);
		world.addPlayer(serverPlayer);
		simulator = new Simulator();
		
		//Setup message handlers
		/*
		 * TODO:This implementation is HORRIBLE. Right now, every instance received every message even if it isn't destined for that instance. 
		 * 		These should be registered in EugServer, and then passed to each instance from there. Or each instance should have its own Server instnace..
		 */
		final Instance thisInst = this;
		EugServer.GetMessageRegistry().listen(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				if(aMsg.getInstanceId() != thisInst.getId()) return;
				if(aMsg.getPlayerId() == -1) return;
				
				//Validate player
				for(Player pl : thisInst.world.getPlayers()){
					if(!pl.isInstanceValid() && pl.getId() == aMsg.getPlayerId()){
						pl.setInstanceValid(true);
					}
				}
			}
		});
		EugServer.GetMessageRegistry().listen(InitializeSceneMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				Player pl = thisInst.findPlayerByConnection(c);
				if(pl != null){
					pl.setInitialized(true);

					
					for(PlayerEventListener listener : Eug.GetPlayerEventListeners()){
						listener.Validated(pl);
					}
				}
			}
		});
		EugServer.GetMessageRegistry().listen(PlayerInput.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				thisInst.clientInputReceived(c, (PlayerInput) msg);			
			}
		});
	}
	
	public void initialize(){}
	
	protected void clientInputReceived(Connection c, PlayerInput input) {	
		world.queueInput(input);
		
		ServerPlayer pl = (ServerPlayer)findPlayerByConnection(c);
		
		if(pl == null) return;
		
		
		long stepMs = (long)(SharedVars.STEP * 1000);
		Snapshot snapshot = world.findPastSnapshot(input.getTimestamp(), stepMs);
		
		if(snapshot != null){			
			snapshot.addInput(input); //Add the input to the snapshot in which the input happened client side
			input.setCorrected(true);
			Snapshot result = simulator.simulate(snapshot, world.getTick(), world.getHistory()); //Simulate a new world based on the client input
			
			world.snapToSnapshot(result); //Apply correction
		}else{
			Eug.GetInputMapper().get(input.getEvent()).resolve(world, input, SharedVars.STEP);	
			input.setCorrected(true);
		}
	}

	public void update(){
		if(simulator.isRunning()){
			if(simulator.getUptime() > (long)1000){
				simulator.terminateSimulation();
				
				Debug.println("[Instance:update] Simulation timedout after running for 1 second.", Debug.Reporting.Fatal);
			}
		}
		
		
		for(Player basePlayer : world.getPlayers()){
			if(basePlayer.getId() == -1) continue; //Skip server player
			ServerPlayer pl = (ServerPlayer)basePlayer;
			//Check for invalid players
			if(!pl.isInstanceValid()){
				//Resend message if enough time has elapsed
				if(Utils.generateTimeStamp() - pl.getValidationTimestamp() >= VALIDATION_DELAY){
					Debug.println("[Instance:update] [" + id + "] resending AssignInstanceMessage to player " + pl.getId() + ".");
					sendAssignInstanceMessage(pl);
				}				
			}else{
				//
				//Send Instance validation
				if(!pl.isInitialized() && Utils.generateTimeStamp() - pl.getValidationTimestamp() >= VALIDATION_DELAY){
					InitializeSceneMessage msg = new InitializeSceneMessage(id, world.getLatestSnapshot());
					msg.sendUDP(pl.getConnection());
					pl.setValidationTimestamp(Utils.generateTimeStamp());
					Debug.println("[Instance:update] [" + id + "] resending InitializeSceneMessage to player " + pl.getId() + ".");
				//
				//Send snapshot
				}else if(pl.isInitialized()){					
					//Send snapshot to each player
					if(pl.isValid()){					
						if((Utils.generateTimeStamp() - pl.getLastSnapshotTimestamp()) >= SNAPSHOT_DELAY && world.getLatestSnapshot().getTimestamp() > pl.getValidationTimestamp()){ //(90 + Math.random() * 110) <- random latency in average latency range
								
							final UpdateMessage msg = new UpdateMessage(world.getLatestSnapshot());
							
							Iterator<PlayerInput> iterator = pl.getInputs().iterator();
							
							while(iterator.hasNext()){
								PlayerInput input = iterator.next();
								if(input.isCorrected()){
									if(input.getSnapshot() != msg.getSnapshot())
										msg.getSnapshot().addInput(input);
									iterator.remove();
								}
							}
							
							pl.setLastSnapshotTimestamp(world.getLatestSnapshot().getTimestamp());
							msg.sendUDP(pl.getConnection());
						}
					}
				}
			}			
			
			//Check for disconnected players
			if(pl.isDisconnected()){
				removePlayerQueue.add(pl);
			}
		}
		
		//Remove disconnected players
		ServerPlayer toRemove = null;
		while((toRemove = removePlayerQueue.poll()) != null){
			removePlayer(toRemove);
		}
		

		world.update(SharedVars.STEP);
	}
	
	private AbstractEntityState[] getEntityStates(){
		AbstractEntityState[] states = new AbstractEntityState[world.countEntities()];
		int i = 0;
		for(NetworkedEntity ent : world.getEntities()){
			try {
				states[i] = ent.getState();
			} catch (NotImplementedException e) {
				e.printStackTrace();
			}
			i++;
		}
		return states;
	}
	
	private int[] getPlayerIds(){
		int[] playerIds = new int[world.countPlayers()];
		int i =0;
		for(Player pl : world.getPlayers()){
			playerIds[i] = pl.getId();
			i++;
		}
		return playerIds;
	}
	
	//Attempts to add player to the instance, returns true on success false if instance if full
	//Additionally sends player a message that he has been added to this instance
	public boolean addPlayer(ServerPlayer player){
		if(isFull()) return false;

		player.setInstanceId(id);
		
		world.addPlayer(player);
		sendAssignInstanceMessage(player);
		
		Debug.println("[Instance:addPlayer] [" + getId() + "] Added player " + player.getId());
		return true;
	}
	
	//Attempts to remove a player from the instance, return true on success, false if player is not in instance
	public boolean removePlayer(ServerPlayer player){
		if(world.removePlayer(player)){
			player.dispose(); //Removes the player's entities
			player.setInstanceId(-1);
			EugServer.QueueIdlePlayer(player);
			Debug.println("[Instance:removePlayer] ["+getId()+"] Removed player " + player.getId());
			return true;
		}
		
		return false;
	}
	
	public NetworkedEntity addEntity(NetworkedEntity ent){
		world.spawn(ent);		
		return ent;
	}
	
	public void removeEntity(NetworkedEntity ent){
		world.destroy(ent);
	}
	
	public void sendAssignInstanceMessage(ServerPlayer pl){
		AssignInstanceMessage msg = new AssignInstanceMessage(id, -1);
		pl.setValidationTimestamp(Utils.generateTimeStamp());
		msg.sendUDP(pl.getConnection());
	}
	
	public boolean isFull(){ return world.countPlayers() == MAX_PLAYERS; }
	public World getWorld(){ return world; }	
	public int getId(){ return id; }

	public ServerPlayer findPlayerByConnection(Connection connection) {
		for(Player pl : world.getPlayers()){
			if(pl.getConnection() == connection)
				return (ServerPlayer)pl;
		}
		
		return null;
	}

	public ServerPlayer findPlayerById(int id) {
		return (ServerPlayer)world.getPlayer(id);
	}
	
	public void dispose(){
		world.dispose();
	}
}
