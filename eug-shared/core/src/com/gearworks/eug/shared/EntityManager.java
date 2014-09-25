package com.gearworks.eug.shared;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Transform;
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
		System.out.println("REGISTERED ENT LISTENER");
		return listener;
	}
	
	public static void RemoveListener(EntityEventListener listener){
		if(listeners == null) return;
		listeners.removeValue(listener, true);
	}
	
	public static void EntityDestroyed(Entity ent){
		if(listeners == null) return;
		for(EntityEventListener listener : listeners){
			listener.onDestroy(ent);
		}
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

		ent.setSpawnState(state);
		Eug.Spawn(ent);
		player.addEntity(ent);
		
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
		
		ent.snapToState(state);
		

		for(EntityEventListener listener : listeners){
			listener.postUpdate(ent);
		}
		
		return ent;
	}
	
	//Creates or destroys an entity based on its state
	public static void UpdateToState(EntityState state, boolean snap, float a) throws EntityBuildException, EntityUpdateException{
		Entity ent = Eug.FindEntityById(state.getId());
		if(ent == null){ 
			ent = BuildFromState(state);
		}else if(ent != null){
			if(snap){
				SnapToState(state, ent);
			}else{
				InterpolateToState(state, ent, a);
			}
		}
	}
	
	public static void UpdateToState(EntityState state, boolean snap) throws EntityBuildException, EntityUpdateException{
		UpdateToState(state, snap, 1);
	}

	public static void InterpolateToState(EntityState state, Entity ent, float a) throws EntityUpdateException {
		if(ent.body().getPosition().equals(state.getBodyState().getTransform().getPosition())) return;
		
		float distance = state.getBodyState().getTransform().getPosition().cpy().sub(ent.body().getPosition()).len(); 
		if(distance < SharedVars.FORCE_POSITION_SNAP_LIMIT && distance > SharedVars.POSITION_TOLERANCE){
			Vector2 currentPosition = ent.body().getPosition();
			Vector2 targetPosition = state.getBodyState().getTransform().getPosition();
			Vector2 smoothPos = currentPosition.cpy().add((targetPosition.cpy().sub(currentPosition).scl(SharedVars.POSITION_TOLERANCE)));
			System.out.println((a) + ": " + currentPosition + " -> " + targetPosition + " = " + smoothPos);
			state.getBodyState().getTransform().setPosition(smoothPos);
		}		
		
		float deltaAngle = Math.abs(state.getBodyState().getTransform().getRotation() - ent.body().getAngle());
		if(deltaAngle < SharedVars.FORCE_ROTATION_SNAP_LIMIT && deltaAngle > SharedVars.ROTATION_TOLERANCE){
			float smoothAngle = ent.body().getAngle() + deltaAngle * (SharedVars.ROTATION_TOLERANCE);
			
			state.getBodyState().getTransform().setRotation(smoothAngle);
		}
		
		SnapToState(state, ent);
	}

	public static void SnapToState(EntityState[] entityStates) throws EntityUpdateException {
		for(int i = 0; i < entityStates.length; i++){
			SnapToState(entityStates[i]);
		}
	}
}
