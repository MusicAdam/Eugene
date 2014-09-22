package com.gearworks.eug.server;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
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
import com.gearworks.eug.shared.entities.LevelBoundsEntity;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.ClientInputMessage;
import com.gearworks.eug.shared.messages.EntityCreatedMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.messages.ClientInputMessage.Event;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.state.Snapshot;

public class Instance {
	public static int MAX_PLAYERS = 4;
	public static long VALIDATION_DELAY = 100; //Time in miliseconds to wait before resending instance validaiton
	public static long SNAPSHOT_DELAY  = 10; //Time in ms to wait before sending new snapshot
	
	private int id;
	private int time;
	private Array<ServerPlayer> 	players;
	private Array<Entity>	entities;
	private World			world;
	private Queue<ServerPlayer> 	removePlayerQueue;
	private Snapshot previousSnapshot;
	private Box2DDebugRenderer b2ddbgRenderer; 
	private ServerPlayer serverPlayer; //This is the player to which level entities belong.
	private Array<Integer> disconnectedPlayerIds;
	
	boolean test = true;
	
	public Instance(int id)
	{
		this.id = id;
		b2ddbgRenderer = new Box2DDebugRenderer();
		removePlayerQueue = new ConcurrentLinkedQueue<ServerPlayer>();
		players = new Array<ServerPlayer>();
		entities = new Array<Entity>();
		world = new World(SharedVars.GRAVITY, SharedVars.DO_SLEEP);
		serverPlayer = new ServerPlayer(-1);
		serverPlayer.setInstanceId(id);
		disconnectedPlayerIds = new Array<Integer>();
		time = 0;
		
		//Setup message handlers
		/*
		 * TODO:This implementation is HORRIBLE. Right now, every instance received every message even if it isn't destined for that instance. 
		 * 		These should be registered in EugServer, and then passed to each instance from there.
		 */
		final Instance thisInst = this;
		EugServer.GetMessageRegistry().register(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
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
		EugServer.GetMessageRegistry().register(InitializeSceneMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				Player pl = thisInst.findPlayerByConnection(c);
				if(pl != null)
					pl.setInitialized(true);				
			}
		});
		EugServer.GetMessageRegistry().register(ClientInputMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				thisInst.clientInputReceived(c, (ClientInputMessage) msg);			
			}
		});
	}
	
	public void initialize(){//init level here
		EugServer.Spawn(new LevelBoundsEntity(entities.size, serverPlayer));
	}
	
	protected void clientInputReceived(Connection c, ClientInputMessage msg) {
		Player pl = findPlayerByConnection(c);
		
		if(pl == null) return;
		
		if(msg.getEvent() == Event.Key){
			if(msg.getKey() == Input.Keys.SPACE){
				((ServerPlayer)pl).getDisk().applyImpulse(msg.getInfoVector());
			}
		}else if(msg.getEvent() == Event.LeftMouseButton){
			((ServerPlayer)pl).getDisk().turnTo(msg.getInfoVector());
		}
	}

	public void update(){
		//Update entities
		for(Entity e : entities){
			e.update();
		}
		//Generate a new snapshot if one is needed
		Snapshot snapshot = null;
		if(previousSnapshot == null || Utils.generateTimeStamp() - previousSnapshot.getTimestamp() >= SNAPSHOT_DELAY){ //Create new snapshot immediately if previous one is null.
			snapshot = new Snapshot(id, time, getPlayerIds(), getDisconnectedPlayerIds(), getEntityStates());
			time++;
			
			if(previousSnapshot == null){
				previousSnapshot = snapshot;
			}
		}
		
		for(int i = 0; i < players.size; i++){
			ServerPlayer pl = players.get(i);
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
					Snapshot useSnap = (snapshot == null) ? previousSnapshot : snapshot;
					InitializeSceneMessage msg = new InitializeSceneMessage(id, useSnap);
					pl.getConnection().sendUDP(msg);
					pl.setValidationTimestamp(Utils.generateTimeStamp());
					Debug.println("[Instance:update] [" + id + "] resending InitializeSceneMessage to player " + pl.getId() + ".");
				//
				//Send snapshot
				}else if(pl.isInitialized()){
					//Initialize player TODO: should be hookable somehow
					if(pl.getDisk() == null){
						//Create disk for player
						Entity e = EugServer.Spawn(new DiskEntity(entities.size, pl));
						pl.setDisk((DiskEntity)e);
					}
					
					//Send snapshot to each player
					if(snapshot != null && pl.isValid()){
						UpdateMessage msg = new UpdateMessage(snapshot);
						pl.getConnection().sendUDP(msg);
						previousSnapshot = snapshot;
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
			disconnectedPlayerIds.add(toRemove.getId()); 
			removePlayer(toRemove);
		}
		
		world.step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
	}
	
	public void render(){
		Gdx.gl.glClearColor(.1f, .1f, .1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		if(SharedVars.DEBUG_PHYSICS){
			Matrix4 dbgMatrix = EugServer.GetCamera().combined.cpy().scl(SharedVars.BOX_TO_WORLD);
			b2ddbgRenderer.render(world, dbgMatrix);
		}
	}
	
	private EntityState[] getEntityStates(){
		EntityState[] states = new EntityState[entities.size];
		for(int i = 0; i < entities.size; i++){
			states[i] = new EntityState(entities.get(i));
		}
		return states;
	}
	
	private int[] getPlayerIds(){
		int[] playerIds = new int[players.size + 1];
		playerIds[0] = serverPlayer.getId();
		for(int i = 0; i < players.size; i++){
			playerIds[i + 1] = players.get(i).getId();
		}
		return playerIds;
	}
	
	private int[] getDisconnectedPlayerIds(){
		int[] dPlayers = new int[disconnectedPlayerIds.size];
		for(int i = 0; i < disconnectedPlayerIds.size; i++){
			dPlayers[i] = disconnectedPlayerIds.get(i);
		}
		return dPlayers;
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
			player.dispose(); //Removes the player's entities
			player.setInstanceId(-1);
			EugServer.QueueIdlePlayer(player);
			Debug.println("[Instance:removePlayer] ["+getId()+"] Removed player " + player.getId());
			return true;
		}
		
		return false;
	}
	
	public Entity addEntity(Entity ent){
		entities.add(ent);		
		ent.getPlayer().addEntity(ent);
		return ent;
	}
	
	public void removeEntity(Entity ent){
		if(entities.removeValue(ent, true)){
			ent.getPlayer().removeEntity(ent);
		}
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

	public ServerPlayer findPlayerById(int id) {
		if(id == serverPlayer.getId()) return serverPlayer;
		for(int i = 0; i < players.size; i++){
			ServerPlayer pl = players.get(i);
			if(pl.getId() == id)
				return pl;
		}
		return null;
	}
}