package com.gearworks.eug.server;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Player;

public class ServerPlayer extends Player {

	public ServerPlayer(Connection conn) {
		super(conn);
	}

}
