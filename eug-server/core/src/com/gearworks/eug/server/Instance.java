package com.gearworks.eug.server;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.World;
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
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.Utils;

public class Instance {
	public static int MAX_PLAYERS = 4;
	public static long VALIDATION_DELAY = 100; //Time in miliseconds to wait before resending instance validaiton
	public static long SNAPSHOT_DELAY  = 100; //Time in ms to wait before sending new snapshot
	
	private int id;
	private int tick;			//Tick indicates a relative time. It is incremented every time a snapshot is generated
	private World			world;
	private Queue<ServerPlayer> 	removePlayerQueue;
	private Box2DDebugRenderer b2ddbgRenderer; 
	private ServerPlayer serverPlayer; //This is the player to which level entities belong.
	private Snapshot lastSnapshotSent;

	public Instance(int id)
	{
		System.out.println("Instance created at: " + Utils.timeToString(Utils.generateTimeStamp()));
		this.id = id;
		b2ddbgRenderer = new Box2DDebugRenderer();
		removePlayerQueue = new ConcurrentLinkedQueue<ServerPlayer>();
		world = new World("InstanceWorld", id);
		serverPlayer = new ServerPlayer(-1);
		serverPlayer.setInitialized(true);
		serverPlayer.setInstanceId(id);
		world.addPlayer(serverPlayer);
		lastSnapshotSent = world.generateSnapshot();
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
				for(Player pl : thisInst.world.getPlayers()){
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
		EugServer.Spawn(new LevelBoundsEntity(world.countEntities(), serverPlayer));
	}
	
	protected void clientInputReceived(Connection c, ClientInput msg) {		
		//ignore if input is from the future
		if(msg.getTick() > tick)
			return;
		
		ServerPlayer pl = (ServerPlayer)findPlayerByConnection(c);
		
		if(pl == null) return;
		
		EugServer.OpenInstanceRequest(id);
		msg.resolve(Eug.GetWorld());
		EugServer.CloseInstanceRequest();
	}

	public void update(){
		tick++;
		world.update(SharedVars.STEP);
		
		//Update entities
		for(Entity e : world.getEntities()){
			e.update();
		}
		
		boolean snapshotSent = false;
		
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
					//Initialize player TODO: should be hookable somehow
					if(pl.getDisk() == null){
						//Create disk for player
						Entity e = EugServer.Spawn(new DiskEntity(world.countEntities(), pl));
						pl.setDisk((DiskEntity)e);
					}
					
					//Send snapshot to each player
					if(pl.isValid()){					
						if((Utils.generateTimeStamp() - pl.getLastSnapshotTimestamp()) >= SNAPSHOT_DELAY && world.getLatestSnapshot().getTimestamp() > pl.getValidationTimestamp()){ //(90 + Math.random() * 110) <- random latency in average latency range
								
							final UpdateMessage msg = new UpdateMessage(world.getLatestSnapshot());
							pl.setLastSnapshotTimestamp(world.getLatestSnapshot().getTimestamp());
							final Player spl = pl;
							new Thread(){
								@Override
								public void run(){
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									msg.sendUDP(spl.getConnection());
								}
							}.start();
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
			lastSnapshotSent = world.getLatestSnapshot();
		}
		
		//Remove disconnected players
		ServerPlayer toRemove = null;
		while((toRemove = removePlayerQueue.poll()) != null){
			removePlayer(toRemove);
		}
		
	}
	
	public void render(){
		Gdx.gl.glClearColor(.1f, .1f, .1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		if(SharedVars.DEBUG_PHYSICS){
			Matrix4 dbgMatrix = EugServer.GetCamera().combined.cpy().scl(SharedVars.BOX_TO_WORLD);
			b2ddbgRenderer.render(world.getPhysicsWorld(), dbgMatrix);
		}
	}
	
	private EntityState[] getEntityStates(){
		EntityState[] states = new EntityState[world.countEntities()];
		int i = 0;
		for(Entity ent : world.getEntities()){
			states[i] = ent.getState();
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
	
	public Entity addEntity(Entity ent){
		world.spawn(ent);		
		return ent;
	}
	
	public void removeEntity(Entity ent){
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
}
