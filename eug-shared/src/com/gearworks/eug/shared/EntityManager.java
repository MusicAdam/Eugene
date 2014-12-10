package com.gearworks.eug.shared;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.state.AbstractEntityState;

public class EntityManager {	
	public static HashMap<NetworkedEntity.Type, Class> registry = new HashMap<NetworkedEntity.Type, Class>();
	
	public static class EntityNotRegisteredException extends Exception{
		private static final long serialVersionUID = 1L;
		
		public EntityNotRegisteredException(String type){
			super(type);
		}
	}
	
	public static void Register(NetworkedEntity.Type typeEnum, Class entityClass){
		registry.put(typeEnum, entityClass);
	}
	
	//Instantiates and spawns an entity
	public static NetworkedEntity Build(NetworkedEntity.Type type, World world, AbstractEntityState state) throws EntityNotRegisteredException{
		NetworkedEntity ent = null;
		Class klass = registry.get(type);
		
		if(klass == null)
			throw new EntityNotRegisteredException(type.toString());
		
		try {
			Constructor ctor = klass.getConstructor(new Class[]{int.class});
			ent = (NetworkedEntity)ctor.newInstance(0);//Eug.GetEntities().size()
			
			if(state != null){
				try {
					ent.snapToState(state);
				} catch (NotImplementedException e) {
					e.printStackTrace();
				}
			}
			
			return world.spawn(ent);
		} catch (NoSuchMethodException e) {
			System.out.println("No valid constructor found for Entity of type " + type);
		}catch(SecurityException e){
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static NetworkedEntity.Type GetEntityType(NetworkedEntity ent){
		Iterator<Entry<NetworkedEntity.Type, Class>> iterator = registry.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<NetworkedEntity.Type, Class> entry = iterator.next();
			
			if(entry.getValue().equals(ent.getClass())){
				return entry.getKey();
			}
		}
		return null;
	}
}
