package com.gearworks.eug.shared;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.state.StateManager;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/*
 * Provides a base class from which shared classes can call methods that should be implemented on both the client and server, but may be implemented differently
 */
public class Eug extends ApplicationAdapter{
	private static Eug singleton;
	public Object playerLock = new Object();
	public Object messageLock = new Object();
	public Object entityLock = new Object();
	
	public static Eug Get()
	{
		return singleton;
	}
	
	public static void Set(Eug e)
	{
		singleton = e;
	}

	protected World getWorld(){ throw new NotImplementedException(); }
	public static World GetWorld()
	{
		return Get().getWorld();
	}
	
	protected StateManager getStateManager(){ throw new NotImplementedException(); }
	public static StateManager GetStateManager()
	{
		return Get().getStateManager();
	}


	protected void destroy(Entity ent){ throw new NotImplementedException(); }
	public static void Destroy(Entity ent)
	{
		EntityManager.EntityDestroyed(ent);
		Get().destroy(ent);
	}

	protected Entity spawn(Entity ent){ throw new NotImplementedException(); }
	public static Entity Spawn(Entity ent)
	{
		return Get().spawn(ent);
	}
	

	protected Map<Integer, Entity> getEntities(){ throw new NotImplementedException(); }
	public static Map<Integer, Entity> GetEntities()
	{
		return Get().getEntities();
	}
	
	public static Entity.Type GetEntityType(Entity ent){
		if(ent instanceof DiskEntity){
			return Entity.Type.DiskEntity;
		}else
			return Entity.Type.BaseEntity;
	}
	
	protected Player findPlayerById(int id){ throw new NotImplementedException(); }
	public static Player FindPlayerById(int id){
		return Get().findPlayerById(id);
	}

	protected Entity findEntityById(int id){ throw new NotImplementedException(); }
	public static Entity FindEntityById(int id) {
		return Get().findEntityById(id);
	}

	/*
	 * Sets the current EntityState. Newly spawned entities should use this state if it is set.
	 */
	protected void setEntityState(EntityState state){ throw new NotImplementedException(); }
	public static void SetEntityState(EntityState state) {
		Get().setEntityState(state);
	}
	
	/*
	 * Returns the connection given by the id if it exists
	 */
	protected Connection getConnectionById(int id){ throw new NotImplementedException(); }
	public static Connection GetConnectionById(int id){ 
		return Get().getConnectionById(id);
	}

	protected boolean entityExists(int id){ throw new NotImplementedException(); }
	public static boolean EntityExists(int id) {
		return Get().entityExists(id);
	}
	
	protected List<Player> getPlayers(){ throw new NotImplementedException(); }
	public static List<Player> GetPlayers(){
		return Get().getPlayers();
	}
}
