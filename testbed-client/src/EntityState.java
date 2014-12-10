import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.state.AbstractEntityState;
import com.gearworks.eug.shared.utils.Vector2;


public class EntityState extends AbstractEntityState {
	public Vector2 position;
	
	public EntityState(Entity entity){
		super(entity);
	}
	
	@Override
	public AbstractEntityState clone() {
		return null;
	}

	@Override
	public boolean epsilonEquals(AbstractEntityState other) {
		return false;
	}

	@Override
	public void fromEntity(NetworkedEntity ent) {
		super.fromEntity(ent);
		
		if(ent instanceof Entity){
			Entity entity = (Entity)ent;
			position = entity.getPosition();
		}
	}

}
