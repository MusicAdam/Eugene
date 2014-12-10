package com.gearworks.eug.shared.state;

import com.badlogic.gdx.InputProcessor;

public interface State {
	public boolean canEnterState();
	public void onEnter();
	public boolean canExitState();
	public void onExit();
	public void render();
	public void update();
	public int getId();
	public void dispose();
}
