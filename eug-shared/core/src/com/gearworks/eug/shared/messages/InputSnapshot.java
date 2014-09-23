package com.gearworks.eug.shared.messages;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.Utils;

public class InputSnapshot extends Message {
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
	private int tick;
	private transient Snapshot snapshot; //The state of the client when this input occured, transient because the server doesn't care about the state of the client, it is just for later reference by the client

	public InputSnapshot(){
		event = null;
		infoVector = null;
		key = -1;
		instanceId = -1;
	}
	
	public InputSnapshot(int instanceId, Snapshot snapshot, Event event, Vector2 infoVector, int key){
		this.timestamp = Utils.generateTimeStamp();
		this.event = event;
		this.infoVector = infoVector;
		this.key = key;
		this.snapshot = snapshot;
		this.tick = snapshot.getTick();
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
	
	public Snapshot getSnapshot(){
		return snapshot;
	}
	
	public int getTick(){
		return tick;
	}
	
	//Do whatever this input was intended to do on the entity
	public void resolve(Entity ent){
		if(ent instanceof DiskEntity){
			DiskEntity disk = (DiskEntity)ent;
			
			if(event == Event.LeftMouseButton)
				disk.turnTo(getInfoVector());
			if(event == Event.Key && key == Input.Keys.SPACE)
				disk.applyImpulse(getInfoVector());
		}
	}
	
}
