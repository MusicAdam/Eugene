package com.gearworks.eug.client;

import com.badlogic.gdx.ApplicationAdapter;
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
import com.esotericsoftware.kryonet.Client;
import com.gearworks.eug.client.entities.ClientEntity;
import com.gearworks.eug.client.state.GameState;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.eug.shared.state.StateManager;

/*
 * Kyro Client wrapper. Handles incoming messages, rendering and updates
 */
public class Eug extends ApplicationAdapter {	
	public static final int 	V_WIDTH = 800;
	public static final	int 	V_HEIGHT = 800;
	private static Eug singletonGame;
	
	private Client client;
	private SpriteBatch batch;
	private StateManager sm;
	private OrthographicCamera camera;
	private World world;
	private ScreenViewport viewport;
	private float accum;
	private Array<ClientEntity> entities;
	private Box2DDebugRenderer b2ddbgRenderer; 
	private ShapeRenderer shapeRenderer;
	private ClientPlayer player;
	private UserInterface ui;
	
	/*
	 * Overrides
	 */
	@Override
	public void create () {
		player = new ClientPlayer();
		ui = new UserInterface();
		
		Gdx.input.setInputProcessor(ui);
		
		/*
		 * Initialize networking infrastructure
		 */
		client = new Client();
		MessageRegistry.Initialize(client.getKryo()); //Register messages
		
		/*
		 * Initialize states		
		 */
		sm = new StateManager();
		sm.setState(new GameState()); //Set initial state
		
		/*
		 * Initialize renderer
		 */
		camera = new OrthographicCamera();
		camera.setToOrtho(false, V_WIDTH, V_HEIGHT);
		camera.update();
		
		viewport = new ScreenViewport(camera);
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		
		entities = new Array<ClientEntity>();
		
		/*
		 * Initialize physics
		 */		
		b2ddbgRenderer = new Box2DDebugRenderer();
		world = new World(new Vector2(0, 0), true);
		//world.setContactListener(new ContactHandler());
		
		player.spawnDisk();
	}

	@Override
	public void render () {
		/*
		 * Handle update logic (should be moved to a separate thread)
		 */

		accum += Gdx.graphics.getDeltaTime();
		while(accum >= SharedVars.STEP) {
			accum -= SharedVars.STEP;
			
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
		
		world.step(SharedVars.STEP, 6, 8);
	}
	
	@Override
	public void resize(int width, int height)
	{
		viewport.update(width, height, true);
	}
	
	public static ClientEntity Spawn(ClientEntity ent)
	{
		Get().entities.add(ent);
		ent.spawn();
		return ent;
	}
	
	public static void Destroy(ClientEntity ent)
	{
		ent.dispose();
		Get().entities.removeValue(ent, true);
	}
	
	/*
	 * Getter/Setters
	 */
	//Singleton getter
	public static Eug Get()
	{
		if(singletonGame == null)
			singletonGame = new Eug();
		
		return singletonGame;
	}
	
	public static Client GetClient()
	{
		return Get().getClient();
	}
	
	public Client getClient()
	{
		return client;
	}
	
	public static StateManager GetStateManager()
	{
		return Get().getStateManager();
	}
	
	public StateManager getStateManager()
	{
		return sm;
	}
	
	public static World GetWorld()
	{
		return Get().world;
	}

	public static Array<ClientEntity> GetEntities() {
		return Get().entities;
	}
	
	public static ShapeRenderer GetShapeRenderer()
	{
		return Get().shapeRenderer;
	}
	
	public static SpriteBatch GetSpriteBatch()
	{
		return Get().batch;
	}
	
	public static ClientPlayer GetPlayer(){
		return Get().player;
	}
	
	public static Camera GetCamera()
	{
		return Get().camera;
	}
	
}
