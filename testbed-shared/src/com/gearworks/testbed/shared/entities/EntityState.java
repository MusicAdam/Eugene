package com.gearworks.testbed.shared.entities;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.state.AbstractEntityState;
import com.gearworks.eug.shared.utils.Vector2;


public class EntityState extends AbstractEntityState {
	public Vector2 position;
	
	public EntityState(){
		super();
	}
	
	public EntityState(Entity entity){
		super(entity);
		position = entity.getPosition();
	}
	
	@Override
	public AbstractEntityState clone() {
		EntityState state = new EntityState();
		state.position 	= position;
		state.id		= id;
		state.timestamp	= timestamp;
		state.playerId	= playerId;
		state.type 		= type;
		state.spriteResource = spriteResource;
		return state;
	}

	@Override
	public boolean epsilonEquals(AbstractEntityState other) {
		if(other == null) return false;
		EntityState state = (EntityState)other;
		return (position.x + SharedVars.POSITION_TOLERANCE <= state.position.x &&
				position.x - SharedVars.POSITION_TOLERANCE >= state.position.x &&
				position.y + SharedVars.POSITION_TOLERANCE <= state.position.y &&
				position.y - SharedVars.POSITION_TOLERANCE >= state.position.y);
	}

}
