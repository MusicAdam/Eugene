package com.gearworks.physics;

import java.util.ArrayList;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

//Provides interface with the world
public class ContactHandler implements ContactListener{

	private ArrayList<Entity> listeners;
	
	
	public ContactHandler() {
		listeners = new ArrayList<Entity>();
	}
	
	public void register(Entity e){
		listeners.add(e);
	}
	
	public void remove(Entity e){
		listeners.remove(e);
	}

	@Override
	public void beginContact(Contact contact) {
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();		
		
		for(Entity ent : listeners){
			if(ent.getBody() == bodyA || ent.getBody() == bodyB)
				ent.beginContact(contact);
		}
	}

	@Override
	public void endContact(Contact contact) {
		if(contact.getFixtureA() == null || contact.getFixtureB() == null) return;
				
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();
		
		for(Entity ent : listeners){
			if(ent.getBody() == bodyA || ent.getBody() == bodyB)
				ent.endContact(contact);
		}	
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();
		
		for(Entity ent : listeners){
			if(ent.getBody() == bodyA || ent.getBody() == bodyB)
				ent.postSolve(contact, impulse);
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold manifold) {
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();
		
		for(Entity ent : listeners){
			if(ent.getBody() == bodyA || ent.getBody() == bodyB)
				ent.preSolve(contact, manifold);
		}
	}

	public void addListener(Entity ent) {
		listeners.add(ent);
	}
	
	public void removeListener(Entity ent){
		listeners.remove(ent);
	}

}
