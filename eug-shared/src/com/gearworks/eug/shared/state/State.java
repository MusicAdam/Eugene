package com.gearworks.eug.shared.state;

public interface State {
	public boolean canEnterState();
	public void onEnter();
	public boolean canExitState();
	public void onExit();
	public void update();
	public int getId();
	public void dispose();
}
