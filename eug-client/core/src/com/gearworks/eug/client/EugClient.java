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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.client.state.ConnectState;
import com.gearworks.eug.client.state.GameState;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.EntityEventListener;
import com.gearworks.eug.shared.EntityManager;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.World;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityUpdateException;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.EntityCreatedMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.state.StateManager;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Kyro Client wrapper. Handles incoming messages, rendering and updates
 */
public class EugClient extends Eug {	
	public static final String 	UPDATE_THREAD = "update";
	public static final int 	V_WIDTH = 800;
	public static final	int 	V_HEIGHT = 800;
	
	private Client client;
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private ScreenViewport viewport;
	private float accum;
	private Box2DDebugRenderer b2ddbgRenderer; 
	private ShapeRenderer shapeRenderer;
	private ClientPlayer player;
	private UserInterface ui;
	protected StateManager sm;
	protected Queue<QueuedMessageWrapper> messageQueue;
	protected Queue<Entity> spawnQueue;
	protected Queue<Entity> destroyQueue;
	protected MessageRegistry messageRegistry;
	protected EntityState entityState;
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
	
	/*
	 * Overrides
	 */
	@Override
	public void create () {	
		Eug.Initialize();
		
		ui = new UserInterface();
		
		Gdx.input.setInputProcessor(ui);
		
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
		
		/*
		 * Initialize renderer
		 */
		camera = new OrthographicCamera();
		camera.setToOrtho(false, V_WIDTH, V_HEIGHT);
		camera.update();
		
		viewport = new ScreenViewport(camera);
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		
		spawnQueue = new ConcurrentLinkedQueue<Entity>();
		destroyQueue = new ConcurrentLinkedQueue<Entity>();
		
		/*
		 * Initialize states		
		 */
		world = new World(-1);
		sm = new StateManager();
		sm.setState(new ConnectState()); //Set initial state
		
		/*
		 * Initialize physics
		 */		
		//world.setContactListener(new ContactHandler());
		b2ddbgRenderer = new Box2DDebugRenderer();
		
		updateThread = new Thread(new Runnable(){
			@Override
			public void run() {
				while(doUpdate){
					accum += Gdx.graphics.getDeltaTime();
					while(accum >= SharedVars.STEP) {
						accum -= SharedVars.STEP;
						//Handle queued messages
						QueuedMessageWrapper message;
						while((message = messageQueue.poll()) != null)
							parseServerMessage(message.connection, message.message);
							
						sm.update();
						//interpolateToTargetSnapshot();
					}
				}
			}		
		});
		updateThread.setName(UPDATE_THREAD);
		//updateThread.start();
	}
	
	//TODO: This doesn't work
	protected void interpolateToTargetSnapshot(){
		if(targetSnapshot == null) return;
		
		float deltaTime = (float)(Utils.generateTimeStamp() - targetSnapshot.getTimestamp());
		for(EntityState state : targetSnapshot.getEntityStates()){
			Entity e = Eug.FindEntityById(state.getId());
			if(e == null) continue;

			float xPos = (state.getBodyState().getTransform().getPosition().x - e.body().getPosition().x) * (1 - deltaTime);
			float yPos = (state.getBodyState().getTransform().getPosition().y - e.body().getPosition().y) * (1 - deltaTime);
			
			System.out.println("Interpolating to: " + state.getBodyState().getTransform().getPosition().x + ", " + e.body().getPosition().x);
			e.position(xPos, yPos);
			e.rotation(state.getBodyState().getTransform().getRotation() * deltaTime + e.body().getAngle() * (1.0f - deltaTime));
		}
	}
	
	public static void SetInstance(int instanceId) {
		EugClient.GetPlayer().setInstanceId(instanceId);
		Eug.GetWorld().setInstanceId(instanceId);
		AssignInstanceMessage msg= new AssignInstanceMessage(instanceId, GetPlayer().getId());
		GetPlayer().getConnection().sendUDP(msg);
		Debug.println("[EugClient:SetInstance] Instance set to " + instanceId);
	}

	@Override
	public void render () {
		/*
		 * Handle update logic 
		 */
		
		dbg_entSearchCount = 0;
		
		accum += Gdx.graphics.getDeltaTime();
		while(accum >= SharedVars.STEP) {
			accum -= SharedVars.STEP;
			//Handle queued messages
			QueuedMessageWrapper message;
			while((message = messageQueue.poll()) != null)
				parseServerMessage(message.connection, message.message);
				
			sm.update();
			//interpolateToTargetSnapshot();
		}
		
		while(!spawnQueue.isEmpty())
			spawn(spawnQueue.poll());
		while(!destroyQueue.isEmpty())
			destroy(destroyQueue.poll());
		
		
		/*
		 * Handle render logic
		 */
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
			b2ddbgRenderer.render(world.getPhysicsWorld(), dbgMatrix);
		}
		
		//fps.log();
		dbg_entSearchSum += dbg_entSearchCount;
		dbg_frameCount++;
		if(Gdx.graphics.getFramesPerSecond() > 0)
			dbg_fpsSum += Gdx.graphics.getFramesPerSecond();
	}
	
	@Override
	public void resize(int width, int height)
	{
		viewport.update(width, height, true);
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
		doUpdate = false;
		Debug.log("");
		Debug.log("Entity Iterations:\t" + dbg_entSearchSum);
		Debug.log("Avg Entity iterations/Frame:\t" + dbg_entSearchSum/dbg_frameCount);
		Debug.log("Average FPS:\t" + dbg_fpsSum/dbg_frameCount);
		Debug.log("Total Frames:\t" + dbg_frameCount);
		Debug.closeLog();
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
	
	public static ShapeRenderer GetShapeRenderer()
	{
		return ((EugClient)Get()).shapeRenderer;
	}
	
	public static SpriteBatch GetSpriteBatch()
	{
		return ((EugClient)Get()).batch;
	}
	
	public static ClientPlayer GetPlayer(){
		return ((EugClient)Get()).player;
	}
	
	@Override
	protected Camera getCamera()
	{
		return camera;
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
	protected Entity spawn(Entity ent)
	{
		world.spawn(ent);
		return ent;
	}
	
	@Override
	protected void destroy(Entity ent)
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
	public Map<Integer, Entity> getEntities() {
		synchronized(entityLock){
			return world.getEntityMap();
		}
	}

	public static void SetPlayer(ClientPlayer clientPlayer) {
		((EugClient)Get()).player = clientPlayer;
		Eug.GetWorld().addPlayer(clientPlayer);
	}
	
	@Override
	public Entity findEntityById(int id){	
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
	public synchronized void setEntityState(EntityState state){
		entityState = state;
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
	protected boolean entityExists(int id){
		return world.getEntityMap().containsKey(id);
	}

	public static int GetInstanceId() {
		return EugClient.GetPlayer().getInstanceId();
	}
	
	@Override 
	protected List<Player> getPlayers(){
		return world.getPlayers();
	}
}
