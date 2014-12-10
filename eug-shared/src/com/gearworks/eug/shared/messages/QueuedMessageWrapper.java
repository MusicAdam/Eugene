package com.gearworks.eug.shared.messages;

import com.esotericsoftware.kryonet.Connection;

//Wraps a message to associate it with a connection in the messagteQueue
public class QueuedMessageWrapper {
	public Connection connection;
	public Message message;

}
