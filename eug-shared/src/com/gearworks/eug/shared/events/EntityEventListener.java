package com.gearworks.eug.shared.events;

import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.state.NetworkedEntityState;

public class EntityEventListener {
	public void onCreate(NetworkedEntity ent){} //An entity was created
	public void preUpdate(NetworkedEntity ent, NetworkedEntityState toState){} //An entity is about to be updated
	public void postUpdate(NetworkedEntity ent){}	//An entity was just updated
	public void onDestroy(NetworkedEntity ent){}
}
