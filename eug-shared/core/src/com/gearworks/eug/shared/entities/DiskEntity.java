package com.gearworks.eug.shared.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.World;

public class DiskEntity extends Entity {
	private Player owner;
	
	private float impulseDelay			= .5f; //Delay between impulses in seconds;
	private float timeSinceLastImpulse 	= impulseDelay; //Initialize with the delay so that an impulse can be applied immediately.
	private Vector2 impulseDirection	= null;		//Impulse is applied in this direction when this is not null (requried to sync IO & box2d)
	private float impulseMagnitude		= 1.2f;	//Magnitude of the impulse
	private float drag					= 500000f;
	private float turnDelay				= .5f; //Delay between turns in seconds
	private float timeSinceLastTurn		= turnDelay; 
	private Vector2 turnToDirection		= null;
	private float angularDamping 		= .2f; //Angular damping;
	private float bumperAngle			= 80; //Bumper half angle in degrees
	
	//Collision information
	private float bumperForceRatio		= .2f; //The fraction of the force applied to bumper collision
	private float nonBumperForceRatio   = 1 - bumperForceRatio;
	private float bumperDamageRatio     = 0f; //The fraction of the damage applied when bumper is hit
	private float nonBumperDamageRatio 	= 1 - bumperDamageRatio;
	private boolean calculatePostsolve	= false; //When true the force ratio will be calculated
	
	private float baseHp				= 100; //Base helath
	private float hp					= baseHp;
	private float baseDamageMod			= 20; //Multiplies base damage
	
	
	public DiskEntity(int id, Player owner) {		
		super(id, owner);
		setType(Type.DiskEntity);
	}
	
	
		
	@Override
	public void render(SpriteBatch batch, ShapeRenderer r){			
		batch.begin();
			sprite.draw(batch);
		batch.end();
		
		super.render(batch, r);
	}
	
	@Override
	public void update(){
		super.update();
		
		timeSinceLastImpulse += SharedVars.STEP;
		timeSinceLastTurn    += SharedVars.STEP;
		
		//Apply impulse after impulse time step to provide more accurate delta time
		if(impulseDirection != null){
			body().setLinearVelocity(new Vector2()); //Zero out the velocity first so disk moves in correct direction.
			body().applyLinearImpulse(impulseMagnitude * impulseDirection.x, impulseMagnitude * impulseDirection.y, body().getPosition().x, body().getPosition().y, true);
			timeSinceLastImpulse = 0;
			impulseDirection = null;
		}
		
		//Apply drag if moving
		if(body().getLinearVelocity().len() > 0){
			body().applyForceToCenter(body().getLinearVelocity().scl(-1/drag), true);
		}
		
		//Apply turn after time step
		if(turnToDirection != null){
			body().setAngularVelocity(0); //Zero angular velocity 
			rotation(turnToDirection.angle());
			turnToDirection = null;
			timeSinceLastTurn = 0;
		}
	}
	
	public void applyImpulse(Vector2 dir){
		if(!canApplyImpulse()) return;
		
		impulseDirection = dir;
	}
	
	public boolean canApplyImpulse(){
		return (timeSinceLastImpulse >= impulseDelay);
	}
	
	public void turnTo(Vector2 dir){
		if(!canDoTurn()) return;
		turnToDirection = dir;
	}
	
	public boolean canDoTurn(){
		return (timeSinceLastTurn >= turnDelay);
	}
	
	@Override
	public void spawn(World world){
		setSprite("disk.png");
		
		//Create body def
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(100 * SharedVars.WORLD_TO_BOX, 100 * SharedVars.WORLD_TO_BOX);
		
		//Create body
		body(world.getPhysicsWorld().createBody(bodyDef));
		body().setAngularDamping(angularDamping);
		body().setUserData(this);
		
		//Create Fixture
		CircleShape unitShape = new CircleShape();
		unitShape.setRadius((sprite.getWidth()/2) * SharedVars.WORLD_TO_BOX );
		
		FixtureDef fix = new FixtureDef();
		fix.shape = unitShape;
		fix.density = 1.0f;
		fix.friction = 0.5f;
		fix.restitution = .9f;
		
		Fixture fixture = body().createFixture(fix);
		fixture.setUserData(this);
		
		unitShape.dispose();
		super.spawn(world);
	}
	
	//This function only checks the angle of the point relative to the position of the body. It does not check that the point is contained within a bodies' fixture
	public boolean worldPointIsOnBumper(Vector2 point){
		Vector2 dir = point.cpy().sub(position()).nor();
		float deltaAngle = rotation() - dir.angle();
		if(deltaAngle >= -bumperAngle && deltaAngle <= bumperAngle){
			return true;
		}
		return false;
	}
	
	@Override
	public void beginContactWith(Fixture myFix, Fixture otherFix, Contact contact){
		float damageRatio = nonBumperDamageRatio;
		
		for(Vector2 point : contact.getWorldManifold().getPoints()){
			if(myFix.testPoint(point)){
				if(worldPointIsOnBumper(point.scl(SharedVars.BOX_TO_WORLD))){
					damageRatio = bumperDamageRatio;
					break;
				}
			}
		}
		
		float baseDamage = body().getLinearVelocity().len() + otherFix.getBody().getLinearVelocity().len();
		baseDamage *= SharedVars.BOX_TO_WORLD * baseDamageMod;
		baseDamage /= baseHp;
		
		applyDamage(baseDamage * damageRatio);
		calculatePostsolve = true;
	}
	
	@Override 
	public void postsolveContactWith(Fixture myFix, Fixture otherFix, Contact contact){
		if(!calculatePostsolve) return;
		
		float forceRatio = nonBumperForceRatio;
		
		for(Vector2 point : contact.getWorldManifold().getPoints()){
			if(myFix.testPoint(point)){
				if(worldPointIsOnBumper(point.scl(SharedVars.BOX_TO_WORLD))){
					forceRatio = bumperForceRatio;
					break;
				}
			}
		}
		
		body().setLinearVelocity(body().getLinearVelocity().scl(forceRatio));
		calculatePostsolve = false;
	}
	
	public void applyDamage(float dmg){
		hp -= dmg;

		if(hp < 0)
			hp = 0;
	}
}
