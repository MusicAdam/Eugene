package com.gearworks.eug.shared;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.entities.LevelBoundsEntity;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityUpdateException;
import com.gearworks.eug.shared.state.BodyState;
import com.gearworks.eug.shared.state.EntityState;

public class EntityManager {
	private static Array<EntityEventListener> listeners;
	
	/*
	 * Factory listeners to hook into entity creation/updates
	 */
	public static EntityEventListener AddListener(EntityEventListener listener){
		if(listeners == null)
			listeners = new Array<EntityEventListener>();
		listeners.add(listener);
		return listener;
	}
	
	public static void RemoveListener(EntityEventListener listener){
		if(listeners == null) return;
		listeners.removeValue(listener, true);
	}
	
	/*
	 * Builds a new entity from a given EntityState
	 */
	public static Entity BuildFromState(EntityState state) throws EntityBuildException, EntityUpdateException{
		//First check if the entity already exists
		if(Eug.FindEntityById(state.getId()) != null) throw new EntityBuildException("Couldn't build entity " + state.getId() + ": Entity already exists");
		
		Entity ent;		
		Player player = Eug.FindPlayerById(state.getPlayerId());
		
		if(player == null) throw new EntityBuildException("Could not build entity " + state.getId() + ": Player " + state.getPlayerId() + " doesn't exist.");
		
		//Invoke the correct constructor based on the Entity.Type given by state
		switch(state.getType()){
			case DiskEntity:
				ent = new DiskEntity(state.getId(), player);
				break;
			case BaseEntity:
				ent = new Entity(state.getId(), player);
				break;
			case LevelBoundsEntity:
				ent = new LevelBoundsEntity(state.getId(), player);
				break;
			default:
				throw new EntityBuildException("Could not build entity " + state.getId() + ": Unknown entity type given.");
		}
		
		Eug.SetEntityState(state);
		Eug.Spawn(ent);
		Eug.SetEntityState(null);
		player.addEntity(ent);
		SnapToState(state);
		
		for(EntityEventListener listener : listeners){
			listener.onCreate(ent);
		}
		
		return ent;
	}
	
	/*
	 * Updates an already created (and spawned) entity to match the given state
	 */
	public static Entity SnapToState(EntityState state) throws EntityUpdateException{//Overload for when an entity is not already known
		Entity ent = Eug.FindEntityById(state.getId());
		if(ent == null) throw new NullPointerException("Could not update entity " + state.getId() + ": Entity does not exist");
		
		return SnapToState(state, ent);
	}
		
	public static Entity SnapToState(EntityState state, Entity ent) throws EntityUpdateException{
		for(EntityEventListener listener : listeners){
			listener.preUpdate(ent, state);
		}
		
		//Update entity's owner
		if(ent.getPlayer().getId() != state.getPlayerId()){
			Player player = Eug.FindPlayerById(state.getPlayerId());
			
			if(player == null) throw new EntityUpdateException("Could not update entity " + state.getId() + ": New player " + state.getPlayerId() + " doesn't exist");
			
			ent.setPlayer(player);
		}
		
		//Update sprite
		if(ent.getSpriteResource() != state.getSpriteResource()){
			ent.setSprite(state.getSpriteResource());
		}
		
		//Update fixtures
		//TODO: I don't think this method of id'ing will work (need to set it in UserData probably)
		//TODO: Will not destroy fixtures if they no longer exist... related to ^^
		for(NetworkedFixture nFix : state.getBodyState().getFixtureList()){
			try{
				Fixture fix = ent.body().getFixtureList().get(nFix.getId());
				fix.setDensity(nFix.getDensity());
				fix.setFilterData(nFix.getFilterData());
				fix.setFriction(nFix.getFriction());
				fix.setRestitution(nFix.getRestitution());
			}catch(IndexOutOfBoundsException e){//Create it if it doesn't exist
				FixtureDef fixDef = new FixtureDef();
				fixDef.friction = nFix.getFriction();
				fixDef.density = nFix.getDensity();
				fixDef.restitution = nFix.getRestitution();
				fixDef.shape = nFix.getShape();
				
				Fixture fix = ent.body().createFixture(fixDef);
				fix.setFilterData(nFix.getFilterData());
			}
		}
		
		//TODO: Update joints
		
		//Apply general physics state
		BodyState bodyState = state.getBodyState();
		
		ent.body().setTransform(bodyState.getTransform().getPosition(), bodyState.getTransform().getRotation());
		ent.body().setAngularDamping(bodyState.getAngularDamping());
		ent.body().setAngularVelocity(bodyState.getAngularVelocity());
		ent.body().setGravityScale(bodyState.getGravityScale());
		ent.body().setLinearDamping(bodyState.getLinearDamping());		
		ent.body().setLinearVelocity(bodyState.getLinearVelocity());
		ent.body().setMassData(bodyState.getMassData());
		ent.body().setActive(bodyState.isActive());
		ent.body().setAwake(bodyState.isAwake());
		ent.body().setBullet(bodyState.isBullet());
		ent.body().setFixedRotation(bodyState.isFixedRotation());
		ent.body().setSleepingAllowed(bodyState.isSleepingAllowed());
		

		for(EntityEventListener listener : listeners){
			listener.postUpdate(ent);
		}
		
		return ent;
	}
	
	//Creates or destroys an entity based on its state
	public static void UpdateToState(EntityState state, boolean snap) throws EntityBuildException, EntityUpdateException{
		Entity ent = Eug.FindEntityById(state.getId());
		if(ent == null){ 
			if(state.wasCreated())
				ent = BuildFromState(state);
			if(state.wasDestroyed())
				throw new EntityBuildException("Can not destroy an entity that does not exist");
			if(state.wasUpdated())
				throw new EntityBuildException("Can not update an entity that does not exist");
		}else if(ent != null){
			//Trying to create entity that already exists
			if(state.wasCreated())
				throw new EntityUpdateException("Cannot create entity that already exists");
			
			if(state.wasUpdated()){
				if(snap){
					SnapToState(state, ent);
				}else{
					InterpolateToState(state, ent);
				}
			}else if(state.wasDestroyed()){
				Eug.Destroy(ent);
			}
		}
	}

	public static void InterpolateToState(EntityState state, Entity ent) throws EntityUpdateException {
		//current.position = previous.position + (target.position-previous.position) * tightness;
		float distance = state.getBodyState().getTransform().getPosition().cpy().sub(ent.body().getPosition()).len(); 
		if(distance > 1.0f || distance < .01f){
			SnapToState(state, ent);
		}else{
			Vector2 currentPosition = ent.body().getPosition();
			Vector2 targetPosition = state.getBodyState().getTransform().getPosition();
			Vector2 smoothPos = currentPosition.cpy().add((targetPosition.cpy().sub(currentPosition)).scl(.1f));
			
			ent.body().setTransform(smoothPos, ent.body().getAngle());
		}		
	}

	public static void SnapToState(EntityState[] entityStates) throws EntityUpdateException {
		for(int i = 0; i < entityStates.length; i++){
			SnapToState(entityStates[i]);
		}
	}
}
