package com.gearworks.eug.shared.input;

import com.gearworks.eug.shared.World;

public interface InputResolver {
	public void resolve(World world, PlayerInput input);
}
