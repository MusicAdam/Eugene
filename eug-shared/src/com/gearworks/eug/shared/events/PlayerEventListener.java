package com.gearworks.eug.shared.events;

import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.state.AbstractEntityState;

public class PlayerEventListener {
	public void Connected(Player player){}
	public void Disconnected(int id){}
	public void AddedToWorld(Player player){}
	public void RemovedFromWorld(Player player){}
	public void Validated(Player player){}
}
