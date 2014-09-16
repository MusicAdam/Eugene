package com.gearworks.eug.shared;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.JointDef;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.utils.Array;

public class NetworkedJoint {
	long timestamp;
	int otherEntityId;
	Vector2 anchor;
	boolean isCollideConnected;
	JointDef.JointType type;
	boolean isActive;
	
	public static NetworkedJoint[] GenerateList(Entity ent) throws NullPointerException{
		Body body = ent.body();
		if(body == null) return null;
		
		Array<JointEdge> bodyJoints = body.getJointList();
		NetworkedJoint[] list = new NetworkedJoint[bodyJoints.size];
		
		for(int i = 0; i < list.length; i++){
			JointEdge edge = bodyJoints.get(i);
			
			if(edge.other.getUserData() instanceof Entity){
				NetworkedJoint joint = new NetworkedJoint();
				Entity other = (Entity)edge.other.getUserData();
				joint.otherEntityId = other.getId();
				joint.anchor = (edge.joint.getBodyA() == ent.body()) ? edge.joint.getAnchorA() : edge.joint.getAnchorB();
				joint.isCollideConnected = edge.joint.getCollideConnected();
				joint.type = edge.joint.getType();
				joint.isActive = edge.joint.isActive();
				list[i] = joint;
			}else{
				throw new NullPointerException("[NetworkedJoint:GenerateList] Couldn't generate NetworkedJoint from body definition: user data is not set to parent entity.");
			}
		}
		
		return list;
	}
	
	public NetworkedJoint(){
		timestamp = Utils.generateTimeStamp();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getOtherEntityId() {
		return otherEntityId;
	}

	public Vector2 getAnchor() {
		return anchor;
	}

	public boolean isCollideConnected() {
		return isCollideConnected;
	}

	public JointDef.JointType getType() {
		return type;
	}

	public boolean isActive() {
		return isActive;
	}
	
	
}
