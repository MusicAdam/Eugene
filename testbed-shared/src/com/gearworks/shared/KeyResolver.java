package com.gearworks.shared;

import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.World;
import com.gearworks.eug.shared.input.InputResolver;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.testbed.shared.entities.Entity;
import static org.lwjgl.glfw.GLFW.*;

public class KeyResolver implements InputResolver {

	@Override
	public void resolve(World world, PlayerInput input) {
		Player pl = world.getPlayer(input.getTargetPlayerID());
		
		if(pl == null)
			return;
		
		pl.addInput(input);
		
		if(GLFW_KEY_D == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x + 1, ent.getPosition().y);
			}
		}
		
		if(GLFW_KEY_W == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x, ent.getPosition().y + 1);
			}
		}
		
		if(GLFW_KEY_A == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x - 1, ent.getPosition().y);
			}
		}
		
		if(GLFW_KEY_S == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x, ent.getPosition().y - 1);
			}
		}
	}
}
