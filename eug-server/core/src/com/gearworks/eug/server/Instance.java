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
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.entities.LevelBoundsEntity;
import com.gearworks.eug.shared.input.ClientInput;
import com.gearworks.eug.shared.input.ClientInput.Event;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.EntityCreatedMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.state.ServerState;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.Utils;

public class Instance {
	public static int MAX_PLAYERS = 4;
	public static long VALIDATION_DELAY = 100; //Time in miliseconds to wait before resending instance validaiton
	public static long SNAPSHOT_DELAY  = 10; //Time in ms to wait before sending new snapshot, new snapshot is sent every tick
	
	private int id;
	private int tick;			//Tick indicates a relative time. It is incremented every time a snapshot is generated
	private Array<ServerPlayer> 	players;
	private Array<Entity>	entities;
	private World			world;
	private Queue<ServerPlayer> 	removePlayerQueue;
	private Box2DDebugRenderer b2ddbgRenderer; 
	private ServerPlayer serverPlayer; //This is the player to which level entities belong.
	private Array<Integer> disconnectedPlayerIds;
	private ServerState previousState;

	public Instance(int id)
	{
		System.out.println("Instance created at: " + Utils.timeToString(Utils.generateTimeStamp()));
		this.id = id;
		b2ddbgRenderer = new Box2DDebugRenderer();
		removePlayerQueue = new ConcurrentLinkedQueue<ServerPlayer>();
		players = new Array<ServerPlayer>();
		entities = new Array<Entity>();
		world = new World(SharedVars.GRAVITY, SharedVars.DO_SLEEP);
		serverPlayer = new ServerPlayer(-1);
		serverPlayer.setInstanceId(id);
		disconnectedPlayerIds = new Array<Integer>();
		tick = 0;
		
		//Setup message handlers
		/*
		 * TODO:This implementation is HORRIBLE. Right now, every instance received every message even if it isn't destined for that instance. 
		 * 		These should be registered in EugServer, and then passed to each instance from there. Or each instance should have its own Server instnace..
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
		EugServer.GetMessageRegistry().register(ClientInput.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				System.out.println("Received input");
				thisInst.clientInputReceived(c, (ClientInput) msg);			
			}
		});
	}
	
	public void initialize(){//init level here
		EugServer.Spawn(new LevelBoundsEntity(entities.size, serverPlayer));
	}
	
	protected void clientInputReceived(Connection c, ClientInput msg) {		
		//ignore if input is from the future
		if(msg.getTick() > tick)
			return;
		
		ServerPlayer pl = (ServerPlayer)findPlayerByConnection(c);
		
		if(pl == null) return;
		
		pl.setInputSnapshot(msg);
		msg.resolve();
	}

	public void update(){
		tick++;
		world.step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
		
		ServerState serverState = new ServerState(id, getPlayerIds(), getDisconnectedPlayerIds(), new Snapshot(id, getPlayerIds(), getEntityStates()));
		serverState.getSnapshot().setServerTick(tick);
		if(previousState == null) previousState = serverState;
		
		//Update entities
		for(Entity e : entities){
			e.update();
		}
		
		boolean snapshotSent = false;
		
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
					InitializeSceneMessage msg = new InitializeSceneMessage(id, serverState);
					msg.sendUDP(pl.getConnection());
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
					if(pl.isValid()){
						if((Utils.generateTimeStamp() - previousState.getTimestamp()) >= SNAPSHOT_DELAY){ //(90 + Math.random() * 110) <- random latency in average latency range
							serverState.setInput(pl.getInputSnapshot());
							UpdateMessage msg = new UpdateMessage(serverState);
							msg.sendUDP(pl.getConnection());
							pl.setInputSnapshot(null);
							snapshotSent = true;
						}
					}
				}
			}			
			
			//Check for disconnected players
			if(pl.isDisconnected()){
				removePlayerQueue.add(pl);
			}
		}
		
		if(snapshotSent){
			previousState = serverState;
		}
		
		//Remove disconnected players
		ServerPlayer toRemove = null;
		while((toRemove = removePlayerQueue.poll()) != null){
			disconnectedPlayerIds.add(toRemove.getId()); 
			removePlayer(toRemove);
		}
		
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
			states[i] = entities.get(i).getState();
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
		msg.sendUDP(pl.getConnection());
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
