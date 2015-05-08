package com.gearworks.testbed.shared.entities;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.state.NetworkedEntityState;
import com.gearworks.eug.shared.utils.Vector2;


public class EntityState extends NetworkedEntityState {
	public Vector2 position;
	
	public EntityState(){
		super();
	}
	
	public EntityState(Entity entity){
		super(entity);
		position = entity.getPosition();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		EntityState state = (EntityState)super.clone();
		state.position = (Vector2)position.clone();
		return state;
	}

	@Override
	public boolean epsilonEquals(NetworkedEntityState other) {
		if(other == null) return false;
		EntityState state = (EntityState)other;
		return (position.x + SharedVars.POSITION_TOLERANCE >= state.position.x &&
				position.x - SharedVars.POSITION_TOLERANCE <= state.position.x &&
				position.y + SharedVars.POSITION_TOLERANCE >= state.position.y &&
				position.y - SharedVars.POSITION_TOLERANCE <= state.position.y &&
				super.epsilonEquals(other));
	}

}
