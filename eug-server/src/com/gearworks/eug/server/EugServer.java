package com.gearworks.eug.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.World;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;
import com.gearworks.eug.shared.state.StateManager;

public class EugServer extends Eug {	
	public static final int 	V_WIDTH = 800;
	public static final	int 	V_HEIGHT = 800;
	
	private Server server;
	private float accum;
	private int requestInstanceId; //Holds reference to the instance we are trying to spawn into
	private boolean isRunning;
	protected StateManager sm;
	protected ArrayList<Instance> instances;
	protected Queue<ServerPlayer> idlePlayers;
	protected World world;
	protected MessageRegistry messageRegistry;
	protected Queue<QueuedMessageWrapper> messageQueue;
	
	/*
	 * Overrides
	 */
	@Override
	public void create() {	
		Eug.Initialize();
		
		/*
		 * Initialize networking infrastructure
		 */
		idlePlayers = new ConcurrentLinkedQueue<ServerPlayer>();
		messageQueue = new ConcurrentLinkedQueue<QueuedMessageWrapper>();
		messageRegistry = new MessageRegistry();
		server = new Server(SharedVars.WRITE_BUFFER_SIZE, SharedVars.OBJECT_BUFFER_SIZE);
		server.addListener(new ServerListener());
		try {
			MessageRegistry.Initialize(server.getKryo()); //Register messages
			
			server.bind(SharedVars.TCP_PORT, SharedVars.UDP_PORT);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		 * Initialize states		
		 */
		sm = new StateManager();
		sm.setState(null); //Set initial state
		
		instances = new ArrayList<Instance>();
		
		isRunning = true;
	}

	@Override
	public void update(float step) {
		//Handle queued messages
		QueuedMessageWrapper message;
		while((message = messageQueue.poll()) != null)
			parseClientMessage(message.connection, message.message);
			
		//TODO: *These things should be moved to a state
		if(!idlePlayers.isEmpty()){
			Instance instance = nextFreeInstance();
			instance = (instance == null) ? createInstance() : instance;
			
			while(!instance.isFull() && !idlePlayers.isEmpty()){
				ServerPlayer pl = idlePlayers.poll();
				
				if(!pl.isDisconnected()){	
					instance.addPlayer(pl);		
				}else{		
					pl.dispose();
					Debug.println("[EugServer] Deleted player " + pl.getId());
				}
			}
		}
			
			
		for(int i = 0; i < instances.size(); i++){
			instances.get(i).update();
		}
		
		sm.update();
	}
	
	public void parseClientMessage(Connection c, Message message)
	{
		Class<?> klass = message.getClass();
		
		for(int i = 0; i < message.getInheritanceLevel(); i++){
			klass = klass.getSuperclass();
		}
		
		messageRegistry.invoke(klass, c, message);
	}
	
	//Gets the next non-full instance
	public Instance nextFreeInstance(){
		for(Instance inst : instances){
			if(!inst.isFull()) return inst;
		}
		
		return null;
	}
	
	public Instance createInstance(){
		Instance inst = new Instance(instances.size());
		instances.add(inst);
		inst.initialize();
		Debug.println("[EugServer:createInstance] Created instance " + inst.getId());
		return inst;
	}
	
	/*
	 * Getter/Setters
	 */
	//Singleton getter
	
	public static Server GetServer()
	{
		return ((EugServer)Get()).getServer();
	}
	
	public Server getServer()
	{
		return server;
	}
	
	@Override
	protected NetworkedEntity spawnEntity(NetworkedEntity ent)
	{
		requestInstanceId = ent.getPlayer().getInstanceId();
		
		Instance instance = instances.get(requestInstanceId); //Needed for ent.spawn();
		
		instance.addEntity(ent);
		
		requestInstanceId = -1;
		return ent;
	}
	
	@Override
	protected void destroyEntity(NetworkedEntity ent)
	{
		Instance instance = instances.get(ent.getInstanceId());
		
		try{
			ent.dispose();
			instance.removeEntity(ent);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	protected World getWorld()
	{
		if(requestInstanceId != -1)
			return instances.get(requestInstanceId).getWorld();
		return null;
	}
	
	@Override
	protected StateManager getStateManager()
	{
		return sm;
	}

	@Override
	public Map<Short, NetworkedEntity> getEntities() {
		HashMap<Short, NetworkedEntity> entityMap = new HashMap<Short, NetworkedEntity>();
		for(int i = 0; i < instances.size(); i++){
			Instance instance = instances.get(i);
			entityMap.putAll(instance.getWorld().getEntityMap());
		}
		return entityMap;
	}
	
	public static void OpenInstanceRequest(int id){
		((EugServer)Eug.Get()).requestInstanceId = id;
	}
	
	public static void CloseInstanceRequest(){
		((EugServer)Eug.Get()).requestInstanceId = -1;
	}
	
	public static MessageRegistry GetMessageRegistry(){
		return ((EugServer)Eug.Get()).messageRegistry;
	}
	
	public static void QueueIdlePlayer(ServerPlayer player){
		((EugServer)Eug.Get()).idlePlayers.add(player);
	}

	
	public static ServerPlayer FindPlayerByConnection(Connection connection) {
		for(ServerPlayer pl : ((EugServer)Get()).idlePlayers){//WARNING: Getting an iterator to ConcurrentQueue is not atomic
			if(pl.getConnection() == connection)
				return pl;
		}
		
		for(Instance inst : ((EugServer)Get()).instances){
			ServerPlayer pl;
			if((pl = inst.findPlayerByConnection(connection)) != null)
				return pl;
		}
		
		return null;
	}

	public static void RemovePlayer(int id) {
		Player pl = FindPlayerById(id);
		RemovePlayer((ServerPlayer)pl);
	}
	
	public static void RemovePlayer(ServerPlayer pl) {
		if(pl == null) return;
		pl.setDisconnected(true); 
	}

	public static void QueueMessage(QueuedMessageWrapper obj) {
		((EugServer)Get()).messageQueue.add(obj);
	}
	
	@Override
	protected Connection getConnectionById(int id){
		for(int i = 0; i < server.getConnections().length; i++){
			if(server.getConnections()[i].getID() == id)
				return server.getConnections()[i];
		}
		return null;
	}
	
	@Override 
	protected Player findPlayerById(int id){
		for(ServerPlayer pl : ((EugServer)Get()).idlePlayers){//WARNING: Getting an iterator to ConcurrentQueue is not atomic
			if(pl.getId() == id)
				return pl;
		}
		
		for(Instance inst : ((EugServer)Get()).instances){
			ServerPlayer pl;
			if((pl = inst.findPlayerById(id)) != null)
				return pl;
		}
		
		return null;
	}
	
	@Override
	protected List<Player> getPlayers(){
		ArrayList<Player> players = new ArrayList<Player>();
		for(int i = 0; i < instances.size(); i++){
			Instance instance = instances.get(i);
			players.addAll(instance.getWorld().getPlayers());
		}
		return players;
	}
	
	public boolean isRunning(){ return isRunning; }
	
	public static Instance GetInstanceByID(int id){
		for(Instance inst : ((EugServer)Get()).instances){
			if(inst.getId() == id)
				return inst;
		}
		return null;
	}
	
	@Override
	public void dispose(){
		for(Instance instance : instances){
			instance.dispose();
		}
		
		while(!idlePlayers.isEmpty()){
			Player pl = idlePlayers.poll();
			pl.dispose();
		}
		
		idlePlayers = null;
		
		server.close();
		server = null;
	}
}
