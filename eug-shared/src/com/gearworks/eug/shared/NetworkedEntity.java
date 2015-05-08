package com.gearworks.eug.shared;

import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.state.NetworkedEntityState;

public class NetworkedEntity {	
	public static final short NETWORKED_ENTITIY = 0x0000;
	
	protected short 		id;
	protected Player 		owner; //The player who owns this entity. Null if it is a world entity (no owner).
	protected String 		spriteResource; //Name of the file from which the sprite's texture was loaded
	private short 			type;
	private NetworkedEntityState snapToState; //Used to snap to a state from another thread.
	
	public NetworkedEntity(short id){
		this.id = id;		
		type = EntityManager.GetEntityType(this);
	}
	
	public Player getOwner(){
		return owner;
	}
	
	public void setOwner(Player player){
		if(this.owner == player) return;
		
		if(this.owner != null)
			this.owner.removeEntity(this);
		
		this.owner = player;
		
		if(this.owner != null)
			this.owner.addEntity(this);
	}
	
	public void setOwner(int playerId){
		setOwner(Eug.GetWorld().getPlayer(playerId));
	}
		
	public void update(){		
		if(snapToState != null){
			snapToState(snapToState);
			snapToState = null;
		}
	}
	
	public void dispose()
	{
		setOwner(null);
	}
	
	public NetworkedEntityState getState() throws NotImplementedException{		
		throw Eug.Get().new NotImplementedException();	}
	
	public void snapToState(NetworkedEntityState state){
		id = state.getId();
		
		if(state.getPlayerId() == -1){
			if(getOwner() != null){
				getOwner().removeEntity(this);
			}
		}else if(state.getPlayerId() != getOwner().getId()){
			Player newPlayer = Eug.GetWorld().getPlayer(state.getPlayerId());
			
			if(newPlayer != null){
				setOwner(newPlayer);
			}
		}
	}
	
	public void setId(short id){ this.id = id; }
	public short getId(){ return id; }
	public String getSpriteResource(){ return spriteResource; }
	public void setSpriteResource(String resource){
		this.spriteResource = resource;
	}	
	public short getType(){ return this.type; }
	
	public void render(){}
}
