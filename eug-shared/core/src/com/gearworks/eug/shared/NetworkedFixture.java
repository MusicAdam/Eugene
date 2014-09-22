package com.gearworks.eug.shared;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Array;
import com.gearworks.eug.shared.utils.Utils;

public class NetworkedFixture {
	long timestamp;
	int id;
	float density;
	float friction;
	float restitution;
	Filter filterData;
	Shape shape;
	
	public static NetworkedFixture[] GenerateList(Body body){
		if(body == null) return null;
		
		Array<Fixture> bodyFixtures = body.getFixtureList();
		NetworkedFixture[] list = new NetworkedFixture[bodyFixtures.size];
		
		for(int i = 0; i < list.length; i++){
			Fixture fixture = bodyFixtures.get(i);
			list[i] = new NetworkedFixture();
			list[i].id = i;
			list[i].density = fixture.getDensity();
			list[i].friction = fixture.getFriction();
			list[i].restitution = fixture.getRestitution();
			list[i].filterData = fixture.getFilterData();
			list[i].shape = fixture.getShape();
		}
		
		return list;
	}
	
	public NetworkedFixture(){
		timestamp = Utils.generateTimeStamp();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public float getDensity() {
		return density;
	}

	public float getFriction() {
		return friction;
	}

	public float getRestitution() {
		return restitution;
	}

	public Filter getFilterData() {
		return filterData;
	}

	public Shape getShape() {
		return shape;
	}
	
	public int getId(){
		return id;
	}
	
	
}
