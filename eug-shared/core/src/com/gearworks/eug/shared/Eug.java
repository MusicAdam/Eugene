package com.gearworks.eug.shared;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.gearworks.eug.shared.state.StateManager;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Eug extends ApplicationAdapter{
	private static Eug singleton;
	
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
		Get().destroy(ent);
	}

	protected Entity spawn(Entity ent){ throw new NotImplementedException(); }
	public static Entity Spawn(Entity ent)
	{
		return Get().spawn(ent);
	}
	

	protected Array<Entity> getEntities(){ throw new NotImplementedException(); }
	public static Array<Entity> GetEntities()
	{
		return Get().getEntities();
	}
}
