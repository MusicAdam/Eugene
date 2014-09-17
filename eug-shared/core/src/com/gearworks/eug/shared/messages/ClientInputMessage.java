package com.gearworks.eug.shared.messages;

import com.badlogic.gdx.math.Vector2;

public class ClientInputMessage extends Message {
	public enum Event{
		LeftMouseButton,
		RightMouseButton,
		Key
	}
	
	private Event event;
	private Vector2 infoVector;
	private int key;
	private int instanceId;

	public ClientInputMessage(){
		event = null;
		infoVector = null;
		key = -1;
		instanceId = -1;
	}
	
	public ClientInputMessage(int instanceId, Event event, Vector2 infoVector, int key){
		this.event = event;
		this.infoVector = infoVector;
		this.key = key;
	}

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
	
}
