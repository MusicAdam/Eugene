package com.gearworks.eug.shared;

import com.gearworks.eug.shared.state.EntityState;

public class EntityEventListener {
	public void onCreate(Entity ent){} //An entity was created
	public void preUpdate(Entity ent, EntityState toState){} //An entity is about to be updated
	public void postUpdate(Entity ent){}	//An entity was just updated
	public void onDestroy(Entity ent){}
}
