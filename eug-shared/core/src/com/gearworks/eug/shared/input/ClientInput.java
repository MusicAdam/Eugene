package com.gearworks.eug.shared.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Represents a client input event.
 * 
 * Records the client tick at which this input occured.
 */
public abstract class ClientInput extends Message {
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
	private int targetPlayerId; //The id of the player to which this input affects.
	
	public ClientInput(){
		event = null;
		infoVector = null;
		key = -1;
		instanceId = -1;
		targetPlayerId = -1;
	}
	
	public ClientInput(int targetPlayerId, Event event, Vector2 infoVector, int key){
		this.timestamp = Utils.generateTimeStamp();
		this.event = event;
		this.infoVector = infoVector;
		this.key = key;
		this.targetPlayerId = targetPlayerId;
	}
	
	public ClientInput(ClientInput cpy){
		this.timestamp = cpy.timestamp;
		this.event = cpy.event;
		this.infoVector = new Vector2(cpy.infoVector.x, cpy.infoVector.y);
		this.key = cpy.key;
		this.targetPlayerId = cpy.targetPlayerId;
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
	
	//Do whatever this input was intended to do on the entity
	public void resolve(){
		Player pl = Eug.FindPlayerById(targetPlayerId);
		if(pl == null){
			throw new NullPointerException("Player " + targetPlayerId + " doesn't exist");
		}
		pl.processInput(this);
	}
		
	//For debug
	@Override
	public String toString(){
		String append = "";
		
		if(event == Event.Key){
			append += key + " " + getInfoVector();
		}else{
			append += getInfoVector();
		}
		
		return "<[" + getTick() + "] " + event + " " + append + ">";
	}
	
}
