package com.gearworks.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gearworks.physics.Entity;
import com.gearworks.physics.PhysicsFactory;

public class Level extends Entity implements Disposable {
	public World world;
	
	public Level(World world){
		super(world, BodyDef.BodyType.StaticBody);
		this.world = world;
		ModelBuilder modelBuilder = new ModelBuilder();
        setModel(modelBuilder.createBox(15f, 1f, 5f, 
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            Usage.Position | Usage.Normal));
	}

	@Override
	public void instantiate(){
		setBody(PhysicsFactory.createStaticBox(world, getModel(), new Vector2(5, 0)));
		modelInstance = new ModelInstance(getModel());
		updateModelTransform();
	}
	
	public ModelInstance getInstance(){
		return modelInstance;
	}

	@Override
	public void beginContact(Contact arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endContact(Contact arg0) {
		// TODO Auto-generated method stub
		
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
