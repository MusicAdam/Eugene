package com.gearworks.eug.shared.input;

import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.World;

public interface InputResolver {
	//Resolves input for a player and returns the player to which the input was related
	public Player resolve(World world, PlayerInput input, float step);
}
