package com.gearworks.eug.shared.state;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.input.ClientInput;
import com.gearworks.eug.shared.utils.Utils;

/*
 * A snapshot contains all the information related to the state of game entities at the time given.
 */
public class Snapshot {
	private int instanceId; //Server instance to which this snapshot is referring
	private long timestamp;
	private int clientTick; //Keeps track of the client tick to which this snapshot refers
	private int serverTick; //  "             "  server  "              "   
	private EntityState[] entityStates; //The states for all the entitites in this instance
	private int[] playerIds; //Players who are connected
	
	public static Snapshot GenerateDefaultSnapshot(Entity ent){
		Snapshot s = new Snapshot();
		s.instanceId = 0;
		s.timestamp = Utils.generateTimeStamp();
		s.entityStates = new EntityState[]{EntityState.GenerateTestState(ent)};
		s.playerIds = new int[]{ent.getPlayer().getId()};
		return s;
	}
	
	public Snapshot(){
		timestamp = Utils.generateTimeStamp();
		clientTick = -1;
		instanceId = -1;
		entityStates = null;
	}

	public Snapshot(int instanceId, int[] playerIds, EntityState[] entityStates) {
		timestamp = Utils.generateTimeStamp();
		this.instanceId = instanceId;
		this.entityStates = entityStates;
	}
	
	public Snapshot(Snapshot cpy){
		this.timestamp = cpy.timestamp;
		this.instanceId = cpy.instanceId;
		this.clientTick = cpy.clientTick;
		this.serverTick = cpy.serverTick;
		
		if(cpy.entityStates != null){
			this.entityStates = new EntityState[cpy.entityStates.length];
			for(int i = 0; i < cpy.entityStates.length; i++){
				this.entityStates[i] = new EntityState(cpy.entityStates[i]);
			}
		}
	}
	
	public int getInstanceId() { return instanceId;	}
	public int getClientTick() { return clientTick; }
	public void setClientTick(int tick){ this.clientTick = tick; }
	public int getServerTick() { return serverTick; }
	public void setServerTick(int tick){ this.serverTick = tick; }
	public long getTimestamp(){ return timestamp; }
	public EntityState[] getEntityStates() { return entityStates; }

	/*
	 * NOTE: Efficiency can be increased by created a hashmap while iterating entityStates to associate id's with indices.
	 * 		probably only worth it if we are expecting to have a lot of entities.
	 */
	public EntityState getEntityState(int id) {
		for(int i = 0; i < entityStates.length; i++){
			if(entityStates[i].getId() == id)
				return entityStates[i];
		}
		System.out.println("ENT " + id + " NOT FOUND");
		return null;
	}

	public static boolean Compare(Snapshot serverSnapshot, Snapshot simulatedState) {		
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
}
