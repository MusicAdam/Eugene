package com.gearworks.eug.shared;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityNotRegisteredException;
import com.gearworks.eug.shared.state.AbstractEntityState;

public class EntityManager {	
	public static HashMap<Short, Class> registry = new HashMap<Short, Class>();
	
	//Register an entity type to its class so that it can be built
	public static void Register(Short typeEnum, Class entityClass){
		registry.put(typeEnum, entityClass);
	}
	
	//Instantiates an entity and adds it to the world.
	public static NetworkedEntity Build(Player player, Short type, AbstractEntityState state) throws EntityNotRegisteredException, EntityBuildException{
		if(Eug.GetWorld().getEntities().size() >= SharedVars.MAX_ENTITIES)
			throw new EntityBuildException("Cannot build new entity: Max entities reached.");
		
		NetworkedEntity ent = null;
		Class klass = registry.get(type);
		
		if(klass == null)
			throw new EntityNotRegisteredException(type.toString());
		
		try {
			Constructor ctor = klass.getConstructor(new Class[]{short.class});
			ent = (NetworkedEntity)ctor.newInstance(Eug.GetWorld().nextEntityID());
			ent.setOwner(player);
			
			if(state != null){
				ent.snapToState(state);
			}
			
			return Eug.GetWorld().spawn(ent);
		} catch (NoSuchMethodException e) {
			throw new EntityBuildException("No valid constructor found for Entity of type " + type);
		}catch(SecurityException e){
			throw new EntityBuildException(e.getMessage());
		} catch (InstantiationException e) {
			throw new EntityBuildException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new EntityBuildException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new EntityBuildException(e.getMessage());
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
			throw new EntityBuildException(e.getMessage());
		}		
	}
	
	public static NetworkedEntity Build(Player player, Short type) throws EntityNotRegisteredException, EntityBuildException{
		return Build(player, type, null);
	}
	
	public static Short GetEntityType(NetworkedEntity ent){
		Iterator<Entry<Short, Class>> iterator = registry.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<Short, Class> entry = iterator.next();
			
			if(entry.getValue().equals(ent.getClass())){
				return entry.getKey();
			}
		}
		return null;
	}
}
