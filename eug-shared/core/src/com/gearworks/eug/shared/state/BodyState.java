package com.gearworks.eug.shared.state;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.Transform;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.NetworkedFixture;
import com.gearworks.eug.shared.NetworkedJoint;
import com.gearworks.eug.shared.Utils;

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
	private Vector2 linearVelocity;
	private MassData massData;
	private boolean isActive;
	private boolean isAwake;
	private boolean isBullet;
	private boolean isFixedRotation;
	private boolean isSleepingAllowed;
	private NetworkedFixture[] fixtureList;
	private NetworkedJoint[] jointList;
	
	public static void FromEntity(Entity ent, BodyState toState){
		Body body = ent.body();
		toState.id = ent.getId();
		toState.transform = body.getTransform();
		toState.angularDamping = body.getAngularDamping();
		toState.angularVelocity = body.getAngularVelocity();
		toState.gravityScale = body.getGravityScale();
		toState.inertia = body.getInertia();
		toState.linearDamping = body.getLinearDamping();
		toState.linearVelocity = body.getLinearVelocity();
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

	public long getTimestamp() {
		return timestamp;
	}

	public Transform getTransform() {
		return transform;
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
		return linearVelocity;
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
