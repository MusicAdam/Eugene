package com.gearworks.game;

import java.util.ArrayList;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL11;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.gearworks.levels.Level;
import com.gearworks.physics.ContactHandler;
import com.gearworks.physics.Entity;

public class Game implements ApplicationListener{
	static final float WORLD_TO_BOX = 0.01f;
	static final float BOX_TO_WORLD = 100f;
	public Environment environment;
    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public CameraInputController camController;
    public ShapeRenderer shapeRenderer;
    public AssetManager assetManager; 
    public Level level;
    public World world;
    public InputHandler inputHandler;
    public ArrayList<Entity> entities;
    Box2DDebugRenderer debugRenderer;
	public ContactHandler contactHandler;

	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "Eugene";
		cfg.width = 800;
		cfg.height = 480;
		LwjglApplication t = new LwjglApplication(new Game(), cfg);
	}

	@Override
	public void create() {
		inputHandler = new InputHandler();
		contactHandler = new ContactHandler();
		world = new World(new Vector2(0, -10), true);
		world.setContactListener(contactHandler);
		debugRenderer = new Box2DDebugRenderer();
		assetManager = new AssetManager();
		shapeRenderer = new ShapeRenderer();
		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));        
		modelBatch = new ModelBatch();
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0, 5, 20f);
        cam.lookAt(0,0,0);
        cam.near = 0.1f;
        cam.far = 50f;
        cam.update();		
        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(inputHandler);
        entities = new ArrayList<Entity>();
        
        level = new Level(world);
        level.instantiate();
        level.setPosition(0, 0);
        addEntity(level);
	}
	
	public void addEntity(Entity ent){
		if(!ent.isInstatiated())
			ent.instantiate();
		entities.add(ent);
		contactHandler.addListener(ent);
	}
	
	public void removeEntity(Entity ent){
		contactHandler.removeListener(ent);
		ent.dispose();
		entities.remove(ent);		
	}

	@Override
	public void resize(int width, int height) {
				
	}
	
	//This is bad
	public void processInput(){
		if(inputHandler.isAction(KeyMapper.Action.Jump)){
			//testChar.doJump();
		}
		
		if(inputHandler.isAction(KeyMapper.Action.MoveLeft)){
			//testChar.doLeft();
		}else if(inputHandler.isAction(KeyMapper.Action.MoveRight)){
			//testChar.doRight();
		}
	}

	@Override
	public void render() {
		processInput();
		
		//Update the camera
		//Vector3 pos = new Vector3();
		//pos = testChar.getInstance().transform.getTranslation(pos);
		//cam.position.set(pos.x, pos.y+5f, pos.z + 20f);
		cam.update();
		
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);		
		
		modelBatch.begin(cam);
		
		for(Entity e : entities){
			e.update();
			modelBatch.render(e.getInstance(), environment);
		}
		
		modelBatch.end();
		
		debugRenderer.render(world, cam.combined);
		
		world.step(1/60f, 6, 2);
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		modelBatch.dispose();
		level.dispose();		
		world.dispose();
	}

}
