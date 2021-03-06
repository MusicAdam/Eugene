package com.gearworks.eug.shared.state;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.PlayerState;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.utils.CircularBuffer;
import com.gearworks.eug.shared.utils.Utils;

/*
 * A snapshot contains all the information related to the state of game entities at the time given.
 */
public class Snapshot {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp;
	private EntityState[] entityStates; //The states for all the entities in the server
	private PlayerState[] players; //Players who are connected	
	private PlayerInput[] inputs; //Record of inputs since last snapshot
	
	public static Snapshot GenerateTestSnapshot(Entity ent){
		Snapshot s = new Snapshot();
		s.instanceId = 0;
		s.timestamp = Utils.generateTimeStamp();
		s.entityStates = new EntityState[]{EntityState.GenerateTestState(ent)};
		s.players = new PlayerState[]{ent.getPlayer().getState()};
		return s;
	}
	
	public Snapshot(){
		timestamp = Utils.generateTimeStamp();
		instanceId = -1;
		entityStates = null;
	}

	public Snapshot(int instanceId, PlayerState[] players, EntityState[] entityStates, PlayerInput[] playerInputs) {
		timestamp = Utils.generateTimeStamp();
		this.instanceId = instanceId;
		this.entityStates = entityStates;
		this.players = players;
		this.inputs = playerInputs;
	}
	
	public Snapshot(Snapshot cpy){
		this.timestamp = cpy.timestamp;
		this.instanceId = cpy.instanceId;
		
		if(cpy.entityStates != null){
			this.entityStates = new EntityState[cpy.entityStates.length];
			for(int i = 0; i < cpy.entityStates.length; i++){
				this.entityStates[i] = new EntityState(cpy.entityStates[i]);
			}
		}
		
		if(cpy.players != null){
			this.players = new PlayerState[cpy.players.length];
			for(int i = 0; i < cpy.players.length; i++){
				this.players[i] = new PlayerState(cpy.players[i]);
			}
		}
		
		if(cpy.inputs != null){
			this.inputs = new PlayerInput[cpy.inputs.length];
			for(int i = 0; i < inputs.length; i++){
				this.inputs[i] = new PlayerInput(cpy.inputs[i]);
			}
		}
	}
	
	public int getInstanceId() { return instanceId;	}
	public long getTimestamp(){ return timestamp; }
	public EntityState[] getEntityStates() { return entityStates; }

	/*
	 * NOTE: Efficiency can be increased by created a hashmap while iterating entityStates to associate id's with indices.
	 * 		probably only worth it if we are expecting to have a lot of entities.
	 */
	public EntityState getEntityState(int id) {
		if(entityStates == null) return null;
		for(int i = 0; i < entityStates.length; i++){
			if(entityStates[i] == null) continue;
			if(entityStates[i].getId() == id)
				return entityStates[i];
		}

		return null;
	}

	public static boolean Compare(Snapshot serverSnapshot, Snapshot simulatedState) {	
		if(serverSnapshot == null || simulatedState == null) return false;
		
		for(int i = 0; i < serverSnapshot.entityStates.length; i++){
			if(!EntityState.Compare(serverSnapshot.entityStates[i], simulatedState.getEntityState(serverSnapshot.entityStates[i].getId())))
				return false;
		}
		return true;
	}
	
	public void render(SpriteBatch batch){
		for(EntityState state : entityStates){
			if(state.getSpriteResource() != null){
				Texture tex = new Texture(state.getSpriteResource());
				Vector2 pos = state.getBodyState().getTransform().getPosition().scl(SharedVars.BOX_TO_WORLD);
				float rot = state.getBodyState().getTransform().getRotation();
				
				batch.begin();
				batch.draw(tex, pos.x - tex.getWidth()/2, pos.y - tex.getHeight()/2, 0, 0, tex.getWidth(), tex.getHeight(), 1, 1, rot, 0, 0, tex.getWidth(), tex.getHeight(), false, false);
				batch.end();
			}
		}
	}

	public void setTimestamp(long time) {
		timestamp = time;
	}

	public PlayerState[] getPlayers() {
		return players;
	}
	
	public PlayerInput[] getInput(){
		return inputs;
	}
	
	public void addInput(PlayerInput input){
		if(input.isSaved()){
			//return; //If this input is already in another snapshot, don't add it to this one.
		}
		
		//Create a new input array with  the snapshots current inputs + 1 for the new input
		int fixedSize = getInput().length + 1;
		PlayerInput[] inputs = new PlayerInput[fixedSize];
		
		//Copy over old inputs
		for(int i = 0; i < getInput().length; i++){
			inputs[i] = getInput()[i];
		}
		
		inputs[fixedSize - 1] = input;
		
		input.setSaved(true);
		
		this.inputs = inputs;
	}
}
