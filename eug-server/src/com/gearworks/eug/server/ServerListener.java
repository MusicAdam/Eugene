package com.gearworks.eug.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.events.PlayerEventListener;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;
import com.gearworks.eug.shared.messages.SecondTestMessage;
import com.gearworks.eug.shared.messages.TestMessage;

public class ServerListener extends Listener {
	@Override
	public void connected(Connection connection)
	{
		ServerPlayer player = new ServerPlayer(connection.getID());
		EugServer.QueueIdlePlayer(player);
		
		for(PlayerEventListener listener : Eug.GetPlayerEventListeners()){
			listener.Connected(player);
		}
	}
	
	@Override
	public void disconnected(Connection connection){		
		for(PlayerEventListener listener : Eug.GetPlayerEventListeners()){
			listener.Disconnected(connection.getID());
		}
		
		EugServer.RemovePlayer(connection.getID());
	}
	
	@Override
	public void received(Connection connection, Object obj)
	{
		if(obj instanceof Message)
		{
			QueuedMessageWrapper w = new QueuedMessageWrapper();
			w.connection = connection;
			w.message = (Message)obj;
			EugServer.QueueMessage(w);
		}
	}
}
