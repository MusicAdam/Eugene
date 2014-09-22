package com.gearworks.eug.shared.messages;

import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.utils.Utils;

public class ClientInputMessage extends Message {
	public enum Event{
		LeftMouseButton,
		RightMouseButton,
		Key
	}
	
	private long timestamp;
	private Event event;
	private Vector2 infoVector;
	private int key;
	private int instanceId;
	private int tick; //The server tick on which this input occured

	public ClientInputMessage(){
		event = null;
		infoVector = null;
		key = -1;
		instanceId = -1;
		tick = -1;
	}
	
	public ClientInputMessage(int instanceId, int tick, Event event, Vector2 infoVector, int key){
		this.timestamp = Utils.generateTimeStamp();
		this.event = event;
		this.infoVector = infoVector;
		this.key = key;
		this.tick = tick;
	}

	public long getTimestamp(){ return timestamp; }
	
	public Event getEvent() {
		return event;
	}

	public Vector2 getInfoVector() {
		return infoVector;
	}

	public int getKey() {
		return key;
	}
	
	public int getInstanceId() {
		return instanceId;
	}	
	
	public int getTick(){
		return tick;
	}
	
}
