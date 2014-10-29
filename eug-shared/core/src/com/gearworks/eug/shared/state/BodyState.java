package com.gearworks.eug.shared.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.Transform;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.NetworkedFixture;
import com.gearworks.eug.shared.NetworkedJoint;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Encapsulates all of the information on the state of the body and its fixtures in the physics simulation
 * 
 * TODO: Encorporate UserData
 */
public class BodyState {
	private long timestamp; 
	private int id; //The id is the same as the id of the parent entity
	private Transform transform;	//Transform of the physics body. null if none exists
	private float angularDamping;
	private float angularVelocity;
	private float gravityScale;
	private float inertia;
	private float linearDamping;
	private float linearVelocityX;
	private float linearVelocityY;
	private MassData massData;
	private boolean isActive;
	private boolean isAwake;
	private boolean isBullet;
	private boolean isFixedRotation;
	private boolean isSleepingAllowed;
	private NetworkedFixture[] fixtureList;
	private NetworkedJoint[] jointList;
	
	public static BodyState GenerateTestState(Entity ent){
		Transform t = new Transform();
		t.setPosition(new Vector2((Gdx.graphics.getWidth()/2) * SharedVars.WORLD_TO_BOX,  (Gdx.graphics.getHeight()/2) * SharedVars.WORLD_TO_BOX));
		t.setRotation(0);
		
		Body body = ent.body();
		BodyState toState = new BodyState();
		toState.id = ent.getId();
		toState.transform = t;
		toState.angularDamping = body.getAngularDamping();
		toState.angularVelocity = body.getAngularVelocity();
		toState.gravityScale = body.getGravityScale();
		toState.inertia = body.getInertia();
		toState.linearDamping = body.getLinearDamping();
		toState.linearVelocityX = 1.2f;
		toState.linearVelocityY = 1.2f;
		toState.massData = body.getMassData();
		toState.isActive = body.isActive();
		toState.isAwake = body.isAwake();
		toState.isBullet = body.isBullet();
		toState.isFixedRotation = body.isFixedRotation();
		toState.isSleepingAllowed = body.isSleepingAllowed();
		
		toState.fixtureList = NetworkedFixture.GenerateList(body);
		toState.jointList = NetworkedJoint.GenerateList(ent);
		return toState;
	}
	
	public static void FromEntity(Entity ent, BodyState toState){
		Body body = ent.body();
		toState.id = ent.getId();
		toState.transform = body.getTransform();
		toState.angularDamping = body.getAngularDamping();
		toState.angularVelocity = body.getAngularVelocity();
		toState.gravityScale = body.getGravityScale();
		toState.inertia = body.getInertia();
		toState.linearDamping = body.getLinearDamping();
		toState.linearVelocityX = body.getLinearVelocity().x;
		toState.linearVelocityY = body.getLinearVelocity().y;
		toState.massData = body.getMassData();
		toState.isActive = body.isActive();
		toState.isAwake = body.isAwake();
		toState.isBullet = body.isBullet();
		toState.isFixedRotation = body.isFixedRotation();
		toState.isSleepingAllowed = body.isSleepingAllowed();
		
		toState.fixtureList = NetworkedFixture.GenerateList(body);
		toState.jointList = NetworkedJoint.GenerateList(ent);
	}
	
	public BodyState(){
		timestamp = Utils.generateTimeStamp();
	}
	
	public BodyState(BodyState cpy){
		this.transform = new Transform();
		this.transform.vals[0] = cpy.transform.vals[0];
		this.transform.vals[1] = cpy.transform.vals[1];
		this.transform.vals[2] = cpy.transform.vals[2];
		this.transform.vals[3] = cpy.transform.vals[3];
		this.id = cpy.id;
		this.angularDamping = cpy.angularDamping;
		this.angularVelocity = cpy.angularVelocity;
		this.gravityScale = cpy.gravityScale;
		this.inertia = cpy.inertia;
		this.linearDamping = cpy.linearDamping;
		this.linearVelocityX = cpy.linearVelocityX;
		this.linearVelocityY = cpy.linearVelocityY;
		this.massData = new MassData();
		this.massData.mass = cpy.massData.mass;
		this.massData.center.x = cpy.massData.center.x;
		this.massData.center.y = cpy.massData.center.y;
		this.massData.I = cpy.massData.I;
		this.isBullet = cpy.isBullet;
		this.isFixedRotation = cpy.isFixedRotation;
		this.isSleepingAllowed = cpy.isSleepingAllowed;
		this.isActive = cpy.isActive;
		this.isAwake = cpy.isAwake;
		
		this.fixtureList = cpy.fixtureList.clone();
		this.jointList = cpy.jointList.clone();
		
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public int getId(){
		return id;
	}

	public Transform getTransform() {
		return transform;
	}
	
	public void setTransform(Transform t){
		this.transform = t;
	}

	public float getAngularDamping() {
		return angularDamping;
	}

	public float getAngularVelocity() {
		return angularVelocity;
	}

	public float getGravityScale() {
		return gravityScale;
	}

	public float getInertia() {
		return inertia;
	}

	public float getLinearDamping() {
		return linearDamping;
	}

	public Vector2 getLinearVelocity() {
		return new Vector2(linearVelocityX, linearVelocityY);
	}

	public MassData getMassData() {
		return massData;
	}

	public boolean isActive() {
		return isActive;
	}

	public boolean isAwake() {
		return isAwake;
	}

	public boolean isBullet() {
		return isBullet;
	}

	public boolean isFixedRotation() {
		return isFixedRotation;
	}

	public boolean isSleepingAllowed() {
		return isSleepingAllowed;
	}

	public NetworkedFixture[] getFixtureList() {
		return fixtureList;
	}

	public NetworkedJoint[] getJointList() {
		return jointList;
	}
}
