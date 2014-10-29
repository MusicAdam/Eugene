package com.gearworks.eug.server;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.messages.EntityCreatedMessage;
import com.gearworks.eug.shared.messages.EntityDestroyedMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;
import com.gearworks.eug.shared.state.StateManager;

public class EugServer extends Eug {	
	public static final int 	V_WIDTH = 800;
	public static final	int 	V_HEIGHT = 800;
	
	private Server server;
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private ScreenViewport viewport;
	private float accum;
	private Box2DDebugRenderer b2ddbgRenderer; 
	private ShapeRenderer shapeRenderer;
	private int requestInstanceId; //Holds reference to the instance we are trying to spawn into
	protected StateManager sm;
	protected Array<Instance> instances;
	protected Queue<ServerPlayer> idlePlayers;
	protected World world;
	protected MessageRegistry messageRegistry;
	protected Queue<QueuedMessageWrapper> messageQueue;
	
	/*
	 * Overrides
	 */
	@Override
	public void create () {	
		
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
		 * Initialize renderer
		 */
		camera = new OrthographicCamera();
		camera.setToOrtho(false, V_WIDTH, V_HEIGHT);
		camera.update();
		
		viewport = new ScreenViewport(camera);
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		
		/*
		 * Initialize states		
		 */
		sm = new StateManager();
		sm.setState(null); //Set initial state
		
		instances = new Array<Instance>();
		
		/*
		 * Initialize physics
		 */		
		world = new World(new Vector2(0, 0), true);
		//world.setContactListener(new ContactHandler());
		b2ddbgRenderer = new Box2DDebugRenderer();
	}

	@Override
	public void render () {
		/*
		 * Handle update logic (should be moved to a separate thread)
		 */
		accum += Gdx.graphics.getDeltaTime();
		while(accum >= SharedVars.STEP) {
			accum -= SharedVars.STEP;

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
			
			//*
			for(int i = 0; i < instances.size; i++){
				instances.get(i).update();
			}
			
			sm.update();
		}
		
		/*
		 * Handle render logic
		 * TODO: Server interface, maybe even render selected instances?
		 */
		
		for(int i = 0; i < instances.size; i++){
			instances.get(i).render();
		}
		
		//TODO: Move to a state to update all instances
		//world.step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
	}
	
	public void parseClientMessage(Connection c, Message message)
	{
		Class<?> klass = message.getClass();
		
		for(int i = 0; i < message.getInheritanceLevel(); i++){
			klass = klass.getSuperclass();
		}
		
		messageRegistry.invoke(klass, c, message);
	}
	
	@Override
	public void resize(int width, int height)
	{
		viewport.update(width, height, true);
	}
	
	//Gets the next non-full instance
	public Instance nextFreeInstance(){
		for(Instance inst : instances){
			if(!inst.isFull()) return inst;
		}
		
		return null;
	}
	
	public Instance createInstance(){
		Instance inst = new Instance(instances.size);
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
	
	public static ShapeRenderer GetShapeRenderer()
	{
		return ((EugServer)Get()).shapeRenderer;
	}
	
	public static SpriteBatch GetSpriteBatch()
	{
		return ((EugServer)Get()).batch;
	}
	
	public static Camera GetCamera()
	{
		return ((EugServer)Get()).camera;
	}
	
	@Override
	protected Entity spawn(Entity ent)
	{
		requestInstanceId = ent.getPlayer().getInstanceId();
		
		Instance instance = instances.get(requestInstanceId); //Needed for ent.spawn();
		
		ent.spawn();
		instance.addEntity(ent);
		
		requestInstanceId = -1;
		return ent;
	}
	
	@Override
	protected void destroy(Entity ent)
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
	public Map<Integer, Entity> getEntities() {
		throw new NotImplementedException();
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
}
