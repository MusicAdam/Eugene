package com.gearworks.characters;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gearworks.game.KeyMapper;
import com.gearworks.levels.Level;
import com.gearworks.physics.Entity;
import com.gearworks.physics.PhysicsFactory;

public class BaseCharacter extends Entity implements Disposable {
	private Level level;
	private boolean onGround;
	private boolean inRight, inLeft;
	public float moveSpeed, jumpSpeed;
	public float mobility;	//This represents the rate at which the character obtains max speed. Delta Speed = moveSpeed*mobility
	public float airMobility; //This represents the ratio of the character's ground speed to their speed in the air. airSpeed = moveSpeed*airMobility
	
	public BaseCharacter(Level level, World world) {
		super(world, BodyDef.BodyType.DynamicBody);

		this.level = level;
		moveSpeed = 5f;
		jumpSpeed = 30f;
		mobility = .8f;
		airMobility = 1f;
		inRight = false;
		inLeft = false;
		
		onGround = false;
		ModelBuilder modelBuilder = new ModelBuilder();
        setModel(modelBuilder.createBox(.5f, 2f, 1f, 
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            Usage.Position | Usage.Normal));
	}
	
	@Override
	public void instantiate() {
		setBody(PhysicsFactory.createDynamicBox(world, getModel(), new Vector2(0, 10), .1f, .5f));
		getBody().setFixedRotation(true);
		modelInstance = new ModelInstance(getModel());
		updateModelTransform();
	}
	
	@Override
	public void update(){
		super.update();
	}
	
	public float calculateSpeed(){
		if(onGround){
			return moveSpeed;
		}else{
			return moveSpeed*airMobility;
		}
	}
	
	public void doLeft(){
		float speed = -calculateSpeed();
		Vector2 vel = getBody().getLinearVelocity();
		//apply acceleration
		speed = vel.x - mobility > speed ? vel.x - mobility : speed;
		float dV = speed - vel.x;
		float impulse = getBody().getMass() * dV;
		getBody().applyLinearImpulse(new Vector2(impulse, 0), getBody().getWorldCenter(), true);
	}
	
	public void doRight(){		
		float speed = calculateSpeed();
		Vector2 vel = getBody().getLinearVelocity();
		//apply acceleration
		speed = vel.x + mobility < speed ? vel.x + mobility : speed;
		float dV = speed - vel.x;
		float impulse = getBody().getMass() * dV;
		getBody().applyLinearImpulse(new Vector2(impulse, 0), getBody().getWorldCenter(), true);
	}
	
	public void doJump(){
		if(onGround){
			getBody().applyForceToCenter(new Vector2(0, jumpSpeed), true);
		}
	}

	@Override
	public void beginContact(Contact c) {
		Body bodyA = c.getFixtureA().getBody();
		Body bodyB = c.getFixtureB().getBody();
		
		if(bodyA == level.getBody() || bodyB == level.getBody()){
			onGround = true;
		}
	}

	@Override
	public void endContact(Contact c) {
		Body bodyA = c.getFixtureA().getBody();
		Body bodyB = c.getFixtureB().getBody();
		
		if(bodyA == level.getBody() || bodyB == level.getBody()){
			onGround = false;
		}
	}

	@Override
	public void postSolve(Contact arg0, ContactImpulse arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preSolve(Contact arg0, Manifold arg1) {
		// TODO Auto-generated method stub
		
	}

}
