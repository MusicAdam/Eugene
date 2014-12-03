package com.gearworks.eug.shared.messages;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.NetworkedFixture;
import com.gearworks.eug.shared.NetworkedJoint;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.PlayerState;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.state.BodyState;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.state.Snapshot;

public class MessageRegistry {	
	private Array<MessageCallback> callbacks;
	
	/*
	 * MessageRegistry.Initialize
	 * 
	 * Description: Registers messages to the kryo serialization library. Should be called on Client and Server to ensure communication.
	 */
	public static void Initialize(Kryo kryo)
	{
		kryo.register(Message.class);
		kryo.register(EntityCreatedMessage.class);
		kryo.register(EntityDestroyedMessage.class);
		kryo.register(UpdateMessage.class);
		kryo.register(AssignInstanceMessage.class);
		kryo.register(BodyState.class);
		kryo.register(EntityState.class);
		kryo.register(EntityState[].class);
		kryo.register(Snapshot.class);
		kryo.register(NetworkedFixture.class);
		kryo.register(NetworkedFixture[].class);
		kryo.register(NetworkedJoint.class);
		kryo.register(NetworkedJoint[].class);
		kryo.register(Vector2.class);
		kryo.register(MassData.class);
		kryo.register(Filter.class);
		kryo.register(CircleShape.class);
		kryo.register(PolygonShape.class);
		kryo.register(EdgeShape.class);
		kryo.register(float[].class);
		kryo.register(Transform.class);
		kryo.register(Entity.Type.class);
		kryo.register(InitializeSceneMessage.class);
		kryo.register(PlayerInput.class);
		kryo.register(PlayerInput.Event.class);
		kryo.register(int[].class);
		kryo.register(PlayerState.class);
		kryo.register(PlayerState[].class);
		kryo.register(ArrayList.class);
		
	}
	
	public MessageRegistry()
	{
		callbacks = new Array<MessageCallback>();
	}
	
	public synchronized int register(Class<?> klass, MessageCallback messageCallback){
		messageCallback.type = klass;
		callbacks.add(messageCallback);
		
		return callbacks.size - 1;
	}
	
	public synchronized void remove(int index){
		callbacks.removeIndex(index);
	}
	
	public void invoke(Class<?> type, Connection c, Message message){
		for(MessageCallback cb : callbacks){
			if(cb.type == type){
				cb.messageReceived(c, message);
			}
		}
	}
}
