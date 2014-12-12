package com.gearworks.shared;

import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.input.InputMapper;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.messages.MessageRegistry;
import com.gearworks.testbed.shared.entities.Entity;
import com.gearworks.testbed.shared.entities.EntityState;

public class Initializer {	
	public static void Initialize(){
		MessageRegistry.RegisterClass(Entity.class);
		MessageRegistry.RegisterClass(EntityState.class);
		
		Eug.SetInputMapper(new InputMapper());
		Eug.GetInputMapper().put(PlayerInput.Event.Key, new KeyResolver());		
	}
}
