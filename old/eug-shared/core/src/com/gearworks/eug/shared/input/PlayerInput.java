package com.gearworks.eug.shared.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.World;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.Utils;

/*
 * Represents a player input event.
 * 
 * This is generated whenever a player makes an input.
 * 
 * TODO: 	A more elegant Resolution method
 * 			Mouse Position instead of "infoVector"
 * 			Support for more inputs
 */
public class PlayerInput extends Message{
	public enum Event{
		LeftMouseButton,
		RightMouseButton,
		Key
	}
	
	private long timestamp;
	private Event event;
	private int key;
	private Vector2 infoVector;
	private int instanceId;
	private int targetPlayerID; //The id of the player who made the input
	
	private transient boolean corrected; 	//Whether a snapshot containing the corrected state at the time of this input has been recieved yet.
											//This is transient because it is a local state that does not need to be transmitted to the server/client
	private transient boolean saved;		//Whether this input has been picked up by a snapshot
											//This is transient because "																"
	
	//Resolves an input
	public static void Resolve(World world, PlayerInput input){
		Player pl = world.getPlayer(input.targetPlayerID);
		
		switch(input.event)
		{
			case LeftMouseButton: 
				pl.resolveLeftMouseButton(input);
				break;
			case RightMouseButton:
				//pl.resolveRightMouseButton(input);
				break;
			case Key:
				pl.resolveKeyPress(input);
				break;
			default:
				//Nothing			
		}
	}
	
	public PlayerInput(){
		event = null;
		infoVector = null;
		key = -1;
		instanceId = -1;
		targetPlayerID = -1;
	}
	
	public PlayerInput(int id, int targetPlayerId, Event event, Vector2 infoVector, int key){
		this.timestamp = Utils.generateTimeStamp();
		this.event = event;
		this.infoVector = infoVector;
		this.key = key;
		this.targetPlayerID = targetPlayerId;
	}
	
	public PlayerInput(PlayerInput cpy){
		this.timestamp = cpy.timestamp;
		this.event = cpy.event;
		this.infoVector = new Vector2(cpy.infoVector.x, cpy.infoVector.y);
		this.key = cpy.key;
		this.targetPlayerID = cpy.targetPlayerID;
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
	
	public int getTargetPlayerID(){ return targetPlayerID; }	
	
	public void setSaved(boolean toggle){ saved = toggle; }
	public boolean isSaved(){ return saved; }
	public void setCorrected(boolean toggle){ corrected = toggle; }
	public boolean isCorrected(){ return corrected; }
}
