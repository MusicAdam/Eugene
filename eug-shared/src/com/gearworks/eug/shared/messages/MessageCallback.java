package com.gearworks.eug.shared.messages;

import com.esotericsoftware.kryonet.Connection;

public class MessageCallback{
	public Class<?> type;
	public void messageReceived(Connection c, Message message){}
}