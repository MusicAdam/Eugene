package com.gearworks.eug.shared;

import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.state.AbstractEntityState;

public class NetworkedEntity {	
	public enum Type {
		BaseEntity,
		DiskEntity,
		LevelBoundsEntity
	}
	
	public static final int COLLISION_NONE = 0;
	public static final int COLLISION_UNIT = 1;
	public static final int COLLISION_WALL = 2;
	
	protected int 		id;
	protected Player player;
	protected String 	spriteResource; //Name of the file from which the sprite's texture was loaded
	private Type type;
	private AbstractEntityState snapToState; //Used to snap to a state from another thread.
	private World world; //The world in which this entity was spawned.
	
	public NetworkedEntity(int id){
		this.id = id;
		type = EntityManager.GetEntityType(this);
	}
	
	public NetworkedEntity(int id, Player player){
		this.id = id;

		type = EntityManager.GetEntityType(this);
		setPlayer(player);
	}
	
	public Player getPlayer(){
		return player;
	}
	
	public void setPlayer(Player player){
		if(this.player == player) return;
		
		if(this.player != null)
			this.player.removeEntity(this);
		
		this.player = player;
		
		if(this.player != null)
			this.player.addEntity(this);
	}
	
	public int getInstanceId(){
		return player.getInstanceId();
	}
	
	
	
	public void update(){		
		if(snapToState != null){
			try {
				snapToState(snapToState);
			} catch (NotImplementedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			snapToState = null;
		}
	}
	
	public void dispose()
	{
		setPlayer(null);
	}
	
	public void spawn(World world){
		this.world = world;
	}
	
	public AbstractEntityState getState() throws NotImplementedException{		
		throw Eug.Get().new NotImplementedException();	}
	
	public void snapToState(AbstractEntityState state) throws NotImplementedException{
		throw Eug.Get().new NotImplementedException();
	}
	
	public void setId(int id){ this.id = id; }
	public int getId(){ return id; }
	public String getSpriteResource(){ return spriteResource; }
	public void setSpriteResource(String resource){
		this.spriteResource = resource;
	}
	public void setType(NetworkedEntity.Type type){ this.type = type; }
	public NetworkedEntity.Type getType(){ return this.type; }
	public World getWorld(){ return world; }
}
