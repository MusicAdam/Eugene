package com.gearworks.eug.shared;

import com.gearworks.eug.shared.state.AbstractEntityState;

public class EntityEventListener {
	public void onCreate(NetworkedEntity ent){} //An entity was created
	public void preUpdate(NetworkedEntity ent, AbstractEntityState toState){} //An entity is about to be updated
	public void postUpdate(NetworkedEntity ent){}	//An entity was just updated
	public void onDestroy(NetworkedEntity ent){}
}
