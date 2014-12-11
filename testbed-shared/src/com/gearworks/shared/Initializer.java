package com.gearworks.shared;

import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.testbed.shared.entities.Entity;
import com.gearworks.testbed.shared.entities.EntityState;

public class Initializer {
	public static void RegisterClasses(){
		MessageRegistry.RegisterClass(Entity.class);
		MessageRegistry.RegisterClass(EntityState.class);
	}
}
