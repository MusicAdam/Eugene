package com.gearworks.eug.server;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
	protected Queue<Message> messageQueue;
	
	/*
	 * Overrides
	 */
	@Override
	public void create () {	
		
		/*
		 * Initialize networking infrastructure
		 */
		idlePlayers = new ConcurrentLinkedQueue<ServerPlayer>();
		messageQueue = new ConcurrentLinkedQueue<Message>();
		messageRegistry = new MessageRegistry();
		server = new Server();
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
			Message message;
			while((message = messageQueue.poll()) != null)
				parseClientMessage(message);
			
			//TODO: *These things should be moved to a state
			if(!idlePlayers.isEmpty()){
				Instance instance = nextFreeInstance();
				instance = (instance == null) ? createInstance() : instance;
				
				while(!instance.isFull() && !idlePlayers.isEmpty()){
					ServerPlayer pl = idlePlayers.poll();
					
					if(pl.getConnection() != null){	
						instance.addPlayer(pl);		
					}else{		
						pl.dispose();
						Debug.println("[EugServer] Deleted player " + pl.getId());
					}
				}
			}
			
			//*
			for(Instance inst : instances){
				inst.update();
			}
			
			sm.update();
		}
		
		/*
		 * Handle render logic
		 * TODO: Server interface, maybe even render selected instances?
		 */
		/*
		Gdx.gl.glClearColor(.1f, .1f, .1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		

		batch.setProjectionMatrix(camera.combined);
		shapeRenderer.setProjectionMatrix(camera.combined);
		try{
			sm.render();
		}catch(NullPointerException e){
			System.out.println("[Eug:Render] NullPointerException encountered. Did you forget to set bodyIsDirty?");
			throw e;
		}
		
		if(SharedVars.DEBUG_PHYSICS){
			Matrix4 dbgMatrix = camera.combined.cpy().scl(SharedVars.BOX_TO_WORLD);
			b2ddbgRenderer.render(world, dbgMatrix);
		}
		*/
		
		//TODO: Move to a state to update all instances
		//world.step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
	}
	
	public void parseClientMessage(Message message)
	{
		messageRegistry.invoke(message.getClass(), message);
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
		
		instance.addEntity(ent);
		ent.spawn();
		
		EntityCreatedMessage msg = new EntityCreatedMessage(ent.getState());
		ent.getPlayer().getConnection().sendUDP(msg);
		
		requestInstanceId = -1;
		return ent;
	}
	
	@Override
	protected void destroy(Entity ent)
	{
		Instance instance = instances.get(ent.getInstanceId());
		

		
		try{
			EntityDestroyedMessage msg = new EntityDestroyedMessage(ent.getId());
			ent.dispose();
			instance.removeEntity(ent);
			ent.getPlayer().getConnection().sendUDP(msg);
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
	public Array<Entity> getEntities() {
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
		for(ServerPlayer pl : ((EugServer)Get()).idlePlayers){//WARNING: Getting an iterator to ConcurrentQueue is not 
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

	public static void RemovePlayerByConnection(Connection connection) {
		ServerPlayer pl = FindPlayerByConnection(connection);
		RemovePlayer(pl);
	}
	
	public static void RemovePlayer(ServerPlayer pl) {
		pl.setConnection(null); //By setting the connection to null, any logic that attempts to use the player will know to remove them.
	}

	public static void QueueMessage(Message obj) {
		((EugServer)Get()).messageQueue.add(obj);
	}
}
