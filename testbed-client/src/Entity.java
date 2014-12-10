import org.lwjgl.opengl.GL11;

import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.state.AbstractEntityState;
import com.gearworks.eug.shared.utils.Vector2;


public class Entity extends NetworkedEntity {
	public static final int WIDTH = 200;
	public static final int HEIGHT = 200;
	Vector2 position;	
	
	public Entity(int id) {
		super(id);
		position = new Vector2(-30, 30);
	}
	
	public void render(){
		GL11.glColor3f(1f,0,0f);
		
		GL11.glBegin(GL11.GL_QUADS);
		    GL11.glVertex2f(position.x,position.y);
		    GL11.glVertex2f(position.x + WIDTH,position.y);
		    GL11.glVertex2f(position.x + WIDTH,position.y+HEIGHT);
		    GL11.glVertex2f(position.x, position.y+HEIGHT);
		GL11.glEnd();
	}
	
	@Override
	public void update(){
		super.update();
	}
	
	
	public Vector2 getPosition(){ return position; }
	public void setPosition(Vector2 pos){ position = pos; }
	
	@Override
	public AbstractEntityState getState() throws NotImplementedException{		
		return new EntityState(this);
	}
	
	@Override
	public void snapToState(AbstractEntityState state) throws NotImplementedException{
		super.snapToState(state);

		EntityState entState = (EntityState)state;
		position = entState.position;
	}
}
