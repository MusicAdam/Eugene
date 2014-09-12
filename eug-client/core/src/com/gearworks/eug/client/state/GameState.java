package com.gearworks.eug.client.state;

import com.gearworks.eug.client.Eug;
import com.gearworks.eug.client.entities.ClientEntity;
import com.gearworks.eug.shared.Debug;
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
		for(ClientEntity e : Eug.GetEntities())
		{ 
			e.render(Eug.GetSpriteBatch(), Eug.GetShapeRenderer());
		}
	}

	@Override
	public void update() {
		for(ClientEntity e : Eug.GetEntities())
		{ 
			e.update();
		}
	}

	@Override
	public int getId() {
		return 1;
	}

}
