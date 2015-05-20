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
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.StateManager;

public class EugServer extends Eug {	
	public static final int 	V_WIDTH = 800;
	public static final	int 	V_HEIGHT = 800;
	
	private Server server;
	private float accum;
	private boolean isRunning;
	protected StateManager sm;
	protected World world;
	protected MessageRegistry messageRegistry;
	protected Queue<QueuedMessageWrapper> messageQueue;
	
	/*
	 * Overrides
	 */
	@Override
	public void create() {
		endpointType = EndpointType.Server;
		Eug.Initialize();
		
		/*
		 * Initialize networking infrastructure
		 */
		world = new World("ServerWorld");
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
		
		messageRegistry.listen(PlayerInput.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message m){
				PlayerInput input = (PlayerInput)m;
				world.queueInput(input);
			}
		});
		
		isRunning = true;
	}

	@Override
	public void update(float step) {
		//Handle queued messages from client
		QueuedMessageWrapper message;
		while((message = messageQueue.poll()) != null)
			parseClientMessage(message.connection, message.message);
			
			
		//Process player updates	
		for(Player pl : world.getPlayers()){		
			UpdateMessage update = new UpdateMessage(world.getLatestSnapshot());
			pl.getConnection().sendUDP(update);
		}
		
		world.update(step);
	}
	
	public void parseClientMessage(Connection c, Message message)
	{
		Class<?> klass = message.getClass();
		
		for(int i = 0; i < message.getInheritanceLevel(); i++){
			klass = klass.getSuperclass();
		}
		
		messageRegistry.invoke(klass, c, message);
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
	protected NetworkedEntity spawn(NetworkedEntity ent)
	{	
		return world.spawn(ent);
	}
	
	@Override
	protected void destroyEntity(NetworkedEntity ent)
	{
		try{
			ent.dispose();
			world.destroy(ent);
		}catch(Exception e){
			e.printStackTrace();
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
		return world.getEntityMap();
	}
	
	public static MessageRegistry GetMessageRegistry(){
		return ((EugServer)Eug.Get()).messageRegistry;
	}
	
	public static ServerPlayer FindPlayerByConnection(Connection connection) {
		for(Player pl : Eug.GetWorld().getPlayers()){//WARNING: Getting an iterator to ConcurrentQueue is not atomic
			if(pl.getConnection() == connection)
				return (ServerPlayer)pl;
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
		for(Player pl : Eug.GetWorld().getPlayers()){//WARNING: Getting an iterator to ConcurrentQueue is not atomic
			if(pl.getId() == id)
				return pl;
		}
		
		return null;
	}
	
	@Override
	protected ArrayList<Player> getPlayers(){
		return Eug.GetWorld().getPlayers();
	}
	
	@Override
	public void dispose(){
		world.dispose();
		
		server.close();
		server = null;
	}
}
