package com.gearworks.eug.client;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.events.PlayerEventListener;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.QueuedMessageWrapper;
import com.gearworks.eug.shared.utils.Utils;

public class ClientListener extends Listener {
	
	public void connected(Connection connection){
		EugClient.SetPlayer(new ClientPlayer(connection.getID()));
		EugClient.SetConnection(connection);
		
		for(PlayerEventListener listener : Eug.GetPlayerEventListeners()){
			listener.Connected(EugClient.GetPlayer());
		}
		Debug.println("[ClientListener:connected] " + connection.getID() + " connected.");
	}
	
	public void disconnected(Connection connection){
		for(PlayerEventListener listener : Eug.GetPlayerEventListeners()){
			listener.Disconnected(connection.getID());
		}
	}
	
	public void received(Connection connection, Object obj)
	{
		//try {
			//Thread.sleep(50);
		//} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
		long time = Utils.generateTimeStamp(); //Get timestamp as soon as the message is received
		if(obj instanceof Message)
		{
			final Object fObj = obj;
			final Connection fConn = connection;
			new java.util.Timer().schedule(
					new java.util.TimerTask(){
						@Override
						public void run(){
							QueuedMessageWrapper w = new QueuedMessageWrapper();
							w.connection = fConn;
							w.message = (Message)fObj;
							w.message.setReceivedTime(Utils.generateTimeStamp());
							EugClient.QueueMessage(w);
						}
					}, 500);
		}
	}
}
