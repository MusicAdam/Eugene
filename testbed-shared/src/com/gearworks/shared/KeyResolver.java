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
	public Player resolve(World world, PlayerInput input, float step) {
		Player pl = world.getPlayer(input.getTargetPlayerID());
		
		if(pl == null)
			return null;
				
		float speed = 100;
		
		if(GLFW_KEY_D == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x + speed * step, ent.getPosition().y);
			}
		}
		
		if(GLFW_KEY_W == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x, ent.getPosition().y + speed * step);
			}
		}
		
		if(GLFW_KEY_A == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x - speed * step, ent.getPosition().y);
			}
		}
		
		if(GLFW_KEY_S == input.getKey()){
			for(NetworkedEntity netEnt : pl.getEntities()){
				Entity ent = (Entity)netEnt;
				ent.setPosition(ent.getPosition().x, ent.getPosition().y - speed * step);
			}
		}
		
		return pl;
	}
}
