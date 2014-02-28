package com.gearworks.physics;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

//Generates Box2d bodies given 3d models
public class PhysicsFactory {
	public static Body createStaticBox(World world, Model model, Vector2 pos){
		// Create our body definition
		BodyDef bodyDef = new BodyDef();  
		// Set its world position
		bodyDef.position.set(pos);  

		// Create a body from the defintion and add it to the world
		Body body = world.createBody(bodyDef);  

		// Create a polygon shape
		PolygonShape box = new PolygonShape();  
		BoundingBox bb = new BoundingBox();
		model.calculateBoundingBox(bb);
		box.setAsBox(bb.max.x, bb.max.y);
		// Create a fixture from our polygon shape and add it to our ground body  
		body.createFixture(box, 0.0f); 
		// Clean up after ourselves
		box.dispose();
		
		return body;
	}
	
	public static Body createDynamicBox(World world, Model model, Vector2 pos, float density, float friction, float restitution){        
		// First we create a body definition
        BodyDef bodyDef = new BodyDef();
        // We set our body to dynamic, for something like ground which doesn't move we would set it to StaticBody
        bodyDef.type = BodyType.DynamicBody;
        // Set our body's starting position in the world
        bodyDef.position.set(pos);

        // Create our body in the world using our body definition
        Body body = world.createBody(bodyDef);

        PolygonShape box = new PolygonShape();  
		BoundingBox bb = new BoundingBox();
		model.calculateBoundingBox(bb);
		box.setAsBox(bb.max.x, bb.max.y);

        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = box;
        fixtureDef.density = density; 
        fixtureDef.friction = friction;
        fixtureDef.restitution = restitution; // Make it bounce a little bit

        // Create our fixture and attach it to the body
        Fixture fixture = body.createFixture(fixtureDef);

        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        box.dispose();
        
        return body;
	}
	
	public static Body createDynamicBox(World world, Model model, Vector2 pos, float density, float friction){
		return createDynamicBox(world, model, pos, density, friction, 0f);
	}
	
	public static Body createDynamicBox(World world, Model model, Vector2 pos, float density){
		return createDynamicBox(world, model, pos, density, 0.3f, 0f);
	}
	
	public static Body createDynamicBox(World world, Model model, Vector2 pos){
		return createDynamicBox(world, model, pos, .5f, 0.3f, 0f);
	}
}
