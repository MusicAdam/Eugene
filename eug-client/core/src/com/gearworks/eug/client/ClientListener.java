package com.gearworks.eug.client;

import com.apple.crypto.provider.Debug;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;

public class ClientListener extends Listener {
	
	public void connected(Connection connection){
		EugClient.SetPlayer(new ClientPlayer(connection.getID()));
		Debug.println("[ClientListener:connected] " + connection.getID() + " connected.");
	}
	
	public void received(Connection connection, Object obj)
	{
		if(obj instanceof Message)
		{
			QueuedMessageWrapper w = new QueuedMessageWrapper();
			w.connection = connection;
			w.message = (Message)obj;
			EugClient.QueueMessage(w);
		}
	}
}
