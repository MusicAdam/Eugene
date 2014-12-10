package com.gearworks.eug.shared.messages;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Base class for messages
 */
public class Message {
	private long sentTime;
	private long receivedTime;
	protected int inheritanceLevel = 0; //If set > 0 will be used to invoke parseMessage on a parent class instead.
	
	public void sendUDP(Connection c){
		this.sentTime = Utils.generateTimeStamp();
		c.sendUDP(this);
	}
	
	public void setReceivedTime(long time){ receivedTime = time; }
	public long getReceivedTime(){ return receivedTime; }
	public long getSentTime(){ return sentTime; }
	public int getInheritanceLevel(){ return inheritanceLevel; }
	public long getTravelTime(){ return receivedTime - sentTime; }
}
