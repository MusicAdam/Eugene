package com.gearworks.eug.client;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.gearworks.eug.shared.messages.Message;

public class ClientListener extends Listener {
	
	public void connected(Connection connection){
		EugClient.SetPlayer(new ClientPlayer(connection));
	}
	
	public void received(Connection connection, Object obj)
	{
		if(obj instanceof Message)
		{
			EugClient.QueueMessage((Message)obj);
		}
	}
}
