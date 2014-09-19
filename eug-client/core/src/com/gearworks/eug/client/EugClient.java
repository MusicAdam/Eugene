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
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.client.state.ConnectState;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.EntityEventListener;
import com.gearworks.eug.shared.EntityFactory;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
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
	protected Queue<QueuedMessageWrapper> messageQueue;
	protected MessageRegistry messageRegistry;
	protected EntityState entityState;
	protected Array<ClientPlayer> otherPlayers; //These are other players connected to the server
	
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
		messageQueue = new ConcurrentLinkedQueue<QueuedMessageWrapper>();
		messageRegistry = new MessageRegistry();
		messageRegistry.register(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				EugClient.SetInstance(aMsg.getInstanceId());
			}
		});
		messageRegistry.register(EntityCreatedMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				((EugClient)Get()).entityCreated((EntityCreatedMessage)msg);
			}
		});
		messageRegistry.register(UpdateMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				((EugClient)Get()).serverUpdate((UpdateMessage)msg);
			}
		});
		messageRegistry.register(InitializeSceneMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				((EugClient)Get()).initializeScene(c, (InitializeSceneMessage)msg);
			}
		});
		
		client = new Client(SharedVars.WRITE_BUFFER_SIZE, SharedVars.OBJECT_BUFFER_SIZE);
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
		
		//Initialize entity listeners
		EntityFactory.AddListener(new EntityEventListener(){
			@Override
			public void onCreate(Entity ent){
				if(ent instanceof DiskEntity){
					if(ent.getPlayer() == player){
						player.setDisk((DiskEntity)ent);
					}
				}
			}
		});
		
		/*
		 * Initialize physics
		 */		
		world = new World(SharedVars.GRAVITY, SharedVars.DO_SLEEP);
		//world.setContactListener(new ContactHandler());
		b2ddbgRenderer = new Box2DDebugRenderer();
	}
	
	protected void initializeScene(Connection c, InitializeSceneMessage msg) {
		if(!player.isInstanceValid()) return;
		if(player.isInitialized()){
			//If for some reason we are still getting this message after we have been initialized and we are initialize, let the server know
			InitializeSceneMessage ack = new InitializeSceneMessage(player.getInstanceId(), null);
			player.getConnection().sendUDP(ack);
			Debug.println("[EugClient:initializeScene] Redundant Scene initialized response sent.");			
		}
		
		for(EntityState state : msg.getSnapshot().getEntityStates()){
			try {
				EntityFactory.BuildFromState(state);
			} catch (EntityBuildException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EntityUpdateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		player.setInitialized(true);
		InitializeSceneMessage ack = new InitializeSceneMessage(player.getInstanceId(), null);
		player.getConnection().sendUDP(ack);
		Debug.println("[EugClient:initializeScene] Scene initialized and response sent.");
	}

	protected void serverUpdate(UpdateMessage msg) {
		if(!player.isInitialized() || !player.isInstanceValid()) return;
		
		//Update players
		for(int playerId : msg.getSnapshot().getPlayerIds()){
			if(playerId == player.getId()) continue;
			if(Eug.FindPlayerById(playerId) == null){
				ClientPlayer pl = new ClientPlayer(playerId);
				otherPlayers.add(pl);
				Debug.println("[EugClient:serverUpdate] Player " + playerId + " connected");
			}
		}
		
		//Check for player removal
		//Do this preliminary check to avoid iterating all players unless absolutely needed
		if(msg.getSnapshot().getPlayerIds().length != otherPlayers.size-1){
			Array<Integer> disconnectedPlayers = new Array<Integer>();
			for(int i = 0; i < otherPlayers.size; i++){
				boolean found = false;
				
				for(int j = 0; j < msg.getSnapshot().getPlayerIds().length; j++){
					if(msg.getSnapshot().getPlayerIds()[j] == otherPlayers.get(i).getId()){
						found = true;
						break;
					}
				}
				
				if(!found){
					disconnectedPlayers.add(otherPlayers.get(i).getId());
				}
			}
			
			for(int i = 0; i < disconnectedPlayers.size; i++){
				otherPlayers.removeValue((ClientPlayer)Eug.FindPlayerById(disconnectedPlayers.get(i)), true);
				Debug.println("[EugClient:serverUpdate] Player " + disconnectedPlayers.get(i) + " disconnected");
				
			}
		}
		
		//Update entities
		for(EntityState state : msg.getSnapshot().getEntityStates()){
			try {
				EntityFactory.UpdateToState(state);
			} catch (EntityUpdateException e) {
				e.printStackTrace();
			}
		}
	}

	protected static void SetInstance(int instanceId) {
		EugClient.GetPlayer().setInstanceId(instanceId);
		AssignInstanceMessage msg= new AssignInstanceMessage(instanceId, GetPlayer().getId());
		GetPlayer().getConnection().sendUDP(msg);
		Debug.println("[EugClient:SetInstance] Instance set to " + instanceId);
	}

	//Handler for message sent by the server that an entity was created
	protected void entityCreated(EntityCreatedMessage msg) {
		try {
			EntityFactory.BuildFromState(msg.getEntityState());
		} catch (EntityBuildException e) {
			e.printStackTrace();
		} catch (EntityUpdateException e) {
			e.printStackTrace();
		}
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
				parseServerMessage(message.connection, message.message);
				
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
	
	public void parseServerMessage(Connection c, Message message)
	{
		messageRegistry.invoke(message.getClass(), c, message);
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
	
	public static void QueueMessage(QueuedMessageWrapper m){
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
	
	@Override
	public Entity findEntityById(int id){
		for(Entity e : entities){
			if(e.getId() == id)
				return e;
		}
		
		return null;
	}
	
	@Override
	public Player findPlayerById(int id){
		if(id == player.getId()) return player;
		
		for(int i = 0; i < otherPlayers.size; i++)
			if(otherPlayers.get(i).getId() == id) 
				return otherPlayers.get(i);
		
		return null;
	}
	
	@Override
	public void setEntityState(EntityState state){
		entityState = state;
	}
	
	@Override
	protected Connection getConnectionById(int id){
		if(id == player.getId())
			return 
		return null; //Other clients can't send messages from our client... ??
	}
}
