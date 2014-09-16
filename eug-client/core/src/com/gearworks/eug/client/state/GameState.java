package com.gearworks.eug.client.state;

import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.state.State;

public class GameState implements State {

	@Override
	public boolean canEnterState() {
		return true;
	}

	@Override
	public void onEnter() {
		Debug.println("GameState: onEnter()");
	}

	@Override
	public boolean canExitState() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onExit() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render() {
		for(Entity e : EugClient.GetEntities())
		{ 
			e.render(EugClient.GetSpriteBatch(), EugClient.GetShapeRenderer());
		}
	}

	@Override
	public void update() {
		for(Entity e : EugClient.GetEntities())
		{ 
			e.update();
		}
	}

	@Override
	public int getId() {
		return 1;
	}

}
