package com.gearworks.testbed.shared.entities;

import org.lwjgl.opengl.GL11;

import com.gearworks.eug.shared.EntityManager;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.state.AbstractEntityState;
import com.gearworks.eug.shared.utils.Vector2;


public class Entity extends NetworkedEntity {
	public static final short ENTITY = 0x0001;
	
	public static final int WIDTH = 30;
	public static final int HEIGHT = 30;
	Vector2 position;	
	
	//Create a shared function to register entities on both client and server
	public static void RegisterEntities(){
		EntityManager.Register(ENTITY, Entity.class);
	}
	
	public Entity(short id, Player player) {
		super(id, player);
		position = new Vector2(0, 0);
	}
	
	@Override
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
	public void snapToState(AbstractEntityState state){
		super.snapToState(state);

		EntityState entState = (EntityState)state;
		position = entState.position;
	}
}
