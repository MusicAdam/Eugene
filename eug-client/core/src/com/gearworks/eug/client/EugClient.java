package com.gearworks.eug.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Client;
import com.gearworks.eug.client.state.ConnectState;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.EntityCreatedMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.eug.shared.state.StateManager;

/*
 * Kyro Client wrapper. Handles incoming messages, rendering and updates
 */
public class EugClient extends Eug {	
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
	protected Array<Entity> entities;
	protected World world;
	protected Queue<Message> messageQueue;
	protected MessageRegistry messageRegistry;
	
	/*
	 * Overrides
	 */
	@Override
	public void create () {	
		ui = new UserInterface();
		
		Gdx.input.setInputProcessor(ui);
		
		/*
		 * Initialize networking infrastructure
		 */
		messageQueue = new ConcurrentLinkedQueue<Message>();
		messageRegistry = new MessageRegistry();
		messageRegistry.register(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				EugClient.SetInstance(aMsg.getInstanceId());
			}
		});
		messageRegistry.register(EntityCreatedMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Message msg){
				((EugClient)Get()).entityCreated((EntityCreatedMessage)msg);
			}
		});
		
		client = new Client();
		client.addListener(new ClientListener());
		MessageRegistry.Initialize(client.getKryo()); //Register messages
		client.start();
		
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
		sm.setState(new ConnectState()); //Set initial state
		
		entities = new Array<Entity>();
		
		/*
		 * Initialize physics
		 */		
		world = new World(SharedVars.GRAVITY, SharedVars.DO_SLEEP);
		//world.setContactListener(new ContactHandler());
		b2ddbgRenderer = new Box2DDebugRenderer();
	}
	
	protected static void SetInstance(int instanceId) {
		EugClient.GetPlayer().setInstanceId(instanceId);
		AssignInstanceMessage msg= new AssignInstanceMessage(instanceId, GetPlayer().getId());
		GetPlayer().getConnection().sendUDP(msg);
		Debug.println("[EugClient:SetInstance] Instance set to " + instanceId);
	}

	//Handler for message sent by the server that an entity was created
	protected void entityCreated(EntityCreatedMessage msg) {
		Debug.println("An entity was created on the server");
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
				parseServerMessage(message);
				
			sm.update();
		}
		
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
			b2ddbgRenderer.render(world, dbgMatrix);
		}
		
		world.step(SharedVars.STEP, SharedVars.VELOCITY_ITERATIONS, SharedVars.POSITION_ITERATIONS);
	}
	
	@Override
	public void resize(int width, int height)
	{
		viewport.update(width, height, true);
	}
	
	public void parseServerMessage(Message message)
	{
		messageRegistry.invoke(message.getClass(), message);
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
	
	public static Camera GetCamera()
	{
		return ((EugClient)Get()).camera;
	}
	
	public static void QueueMessage(Message m){
		((EugClient)Get()).messageQueue.add(m);
	}
	
	
	public static MessageRegistry GetMessageRegistry()
	{
		return ((EugClient)Get()).messageRegistry;
	}
	
	@Override
	protected Entity spawn(Entity ent)
	{
		entities.add(ent);
		ent.spawn();
		return ent;
	}
	
	@Override
	protected void destroy(Entity ent)
	{
		ent.dispose();
		entities.removeValue(ent, true);
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
	public Array<Entity> getEntities() {
		return entities;
	}

	public static void SetPlayer(ClientPlayer clientPlayer) {
		((EugClient)Get()).player = clientPlayer;
	}
	
}
