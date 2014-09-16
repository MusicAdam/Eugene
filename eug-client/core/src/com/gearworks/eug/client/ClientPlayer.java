package com.gearworks.eug.client;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Player;

public class ClientPlayer extends Player {

	public ClientPlayer(Connection conn) {
		super(conn);
	}
}
