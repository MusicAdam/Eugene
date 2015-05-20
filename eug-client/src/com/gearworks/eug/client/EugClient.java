package com.gearworks.eug.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.client.state.ConnectState;
import com.gearworks.eug.client.state.GameState;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.EntityManager;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.World;
import com.gearworks.eug.shared.Eug.EndpointType;
import com.gearworks.eug.shared.events.EntityEventListener;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityUpdateException;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.messages.EntityCreatedMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.NetworkedEntityState;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.state.StateManager;
import com.gearworks.eug.shared.utils.Utils;
import com.gearworks.eug.shared.utils.Vector2;

public class EugClient extends Eug {	
	public static final String 	UPDATE_THREAD = "update";
	public static final int 	V_WIDTH = 800;
	public static final	int 	V_HEIGHT = 800;
	
	private Client client;
	private float accum;
	private ClientPlayer player;
	protected StateManager sm;
	protected Queue<QueuedMessageWrapper> messageQueue;
	protected Queue<NetworkedEntity> spawnQueue;
	protected Queue<NetworkedEntity> destroyQueue;
	protected MessageRegistry messageRegistry;
	protected NetworkedEntityState entityState;
	protected Connection connection; //The player's connection to the server
	protected Snapshot previousSnapshot;
	protected Snapshot targetSnapshot;
	protected World world;
	
	protected Thread updateThread;
	protected boolean doUpdate = true;;
	
	private int dbg_entSearchCount;	
	private int dbg_entSearchSum = 0;	
	private int dbg_frameCount = 0;	
	private int dbg_fpsSum = 0;
	
	@Override
	public void create () {	
		endpointType = EndpointType.Client;
		Eug.Initialize();
		
		/*
		 * Initialize networking infrastructure
		 */
		messageQueue = new ConcurrentLinkedQueue<QueuedMessageWrapper>();
		messageRegistry = new MessageRegistry();
		
		client = new Client(SharedVars.WRITE_BUFFER_SIZE, SharedVars.OBJECT_BUFFER_SIZE);
		client.addListener(new ClientListener());
		MessageRegistry.Initialize(client.getKryo()); //Register messages
		client.start();
		dbg_entSearchCount = 0;
		
		spawnQueue = new ConcurrentLinkedQueue<NetworkedEntity>();
		destroyQueue = new ConcurrentLinkedQueue<NetworkedEntity>();
		
		/*
		 * Initialize states		
		 */
		world = new World("ClientWorld");
		world.setRecordHistory(false); //Don't record history until we recieve the server initialization message.
		sm = new StateManager();
		sm.setState(new ConnectState()); //Set initial state
	}

	@Override
	public void update(float step) {
		/*
		 * Handle update logic 
		 */
		
		dbg_entSearchCount = 0;		
		
		//Handle queued messages
		QueuedMessageWrapper message;
		while((message = messageQueue.poll()) != null)
			parseServerMessage(message.connection, message.message);
		
		sm.update();
		
		while(!spawnQueue.isEmpty())
			spawnEntity(spawnQueue.poll());
		while(!destroyQueue.isEmpty())
			destroyEntity(destroyQueue.poll());
		
	}
	
	public void parseServerMessage(Connection c, Message message)
	{
		synchronized(messageLock){
			Class<?> klass = message.getClass();
			
			for(int i = 0; i < message.getInheritanceLevel(); i++){
				klass = klass.getSuperclass();
			}
			
			messageRegistry.invoke(klass, c, message);
		}
	}
	
	@Override
	public void dispose(){
		sm.dispose();
		client.close();
		client = null;
		doUpdate = false;
	}
	
	/*
	 * Getter/Setters
	 */
	//Singleton getter
	
	public static Client GetClient()
	{
		return ((EugClient)Get()).getClient();
	}
	
	public Client getClient()
	{
		return client;
	}
	
	public static ClientPlayer GetPlayer(){
		return ((EugClient)Get()).player;
	}
	
	public static void QueueMessage(QueuedMessageWrapper m){
		synchronized(Get().messageLock){
			((EugClient)Get()).messageQueue.add(m);
		}
	}
	
	
	public static MessageRegistry GetMessageRegistry()
	{
		synchronized(Get().messageLock){
			return ((EugClient)Get()).messageRegistry;
		}
	}
	
	@Override
	protected NetworkedEntity spawnEntity(NetworkedEntity ent)
	{
		world.spawn(ent);
		return ent;
	}
	
	@Override
	protected void destroyEntity(NetworkedEntity ent)
	{
		if(Thread.currentThread().getName().equals(UPDATE_THREAD)){
			destroyQueue.add(ent);
		}else{
			synchronized(entityLock){
				world.destroy(ent);
			}
		}
	}
	
	@Override
	protected World getWorld()
	{
		return world;
	}
	
	@Override
	protected StateManager getStateManager()
	{
		return sm;
	}

	@Override
	public Map<Short, NetworkedEntity> getEntities() {
		synchronized(entityLock){
			return world.getEntityMap();
		}
	}

	public static void SetPlayer(ClientPlayer clientPlayer) {
		((EugClient)Get()).player = clientPlayer;
		Eug.GetWorld().addPlayer(clientPlayer);
	}
	
	@Override
	public NetworkedEntity findEntityById(short id){	
		synchronized(entityLock){
			dbg_entSearchCount++;
			return world.getEntity(id);
		}
	}
	
	@Override
	public Player findPlayerById(int id){
		synchronized(playerLock){
			return world.getPlayer(id);
		}
	}
	
	@Override
	protected Connection getConnectionById(int id){
		if(id == player.getId())
			return connection;
		throw new NullPointerException("Other clients can't send messages from this client...");
	}

	public static void SetConnection(Connection connection) {
		((EugClient)Get()).connection = connection;
	}
	
	@Override
	protected boolean entityExists(short id){
		return world.getEntityMap().containsKey(id);
	}

	public static int GetInstanceId() {
		return EugClient.GetPlayer().getInstanceId();
	}
	
	@Override 
	protected List<Player> getPlayers(){
		return world.getPlayers();
	}
	
	public static PlayerInput GenerateInput(int targetPlayer, PlayerInput.Event event, Vector2 mouse, int key){
		PlayerInput input = new PlayerInput(-1, targetPlayer, event, mouse, key, Eug.GetWorld().getTick());
		Eug.GetWorld().queueInput(input);		
		return input;
	}
}
