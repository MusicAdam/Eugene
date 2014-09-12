package com.gearworks.eug.client.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.gearworks.eug.client.Eug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.SharedVars;

public class ClientEntity extends Entity {
	protected Sprite 	sprite;
	
	@Override
	public void update(){
		super.update();
		followPhysicsBody();
	}
	
	@Override
	public void spawn(){		
		//Create body def
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(100 * SharedVars.WORLD_TO_BOX, 100 * SharedVars.WORLD_TO_BOX);
		
		//Create body
		body = Eug.GetWorld().createBody(bodyDef);
		
		//Create Fixture
		PolygonShape testBox = new PolygonShape();
		testBox.setAsBox(10 * SharedVars.WORLD_TO_BOX, 10 * SharedVars.WORLD_TO_BOX);
		
		FixtureDef fix = new FixtureDef();
		fix.shape = testBox;
		fix.density = 1.0f;
		fix.friction = 0.1f;
		fix.restitution = 0.0f;
		
		Fixture fixture = body.createFixture(fix);
		fixture.setUserData(this);
		
		testBox.dispose();
		
		body().applyTorque(100, true);
		bodyIsDirty = true;
	}
	
	@Override
	public void dispose()
	{
		if(body == null) return;
		
		Eug.GetWorld().destroyBody(body);
	}
	
	protected void followPhysicsBody(){
		if(sprite == null) return;
		if(body == null) return; 
		
		Vector2 position = position();
		sprite.setPosition(position.x - sprite.getWidth()/2, position.y - sprite.getHeight()/2);
		sprite.setRotation(rotation());
	}

}
