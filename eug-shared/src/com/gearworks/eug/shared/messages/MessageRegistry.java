package com.gearworks.eug.shared.messages;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.PlayerState;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.state.AbstractEntityState;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.Vector2;

public class MessageRegistry {	
	private ArrayList<MessageCallback> callbacks;
	private static ArrayList<Class> userClasses = new ArrayList<Class>();
	
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
		kryo.register(AbstractEntityState.class);
		kryo.register(AbstractEntityState[].class);
		kryo.register(Snapshot.class);
		kryo.register(float[].class);
		kryo.register(short.class);
		kryo.register(InitializeSceneMessage.class);
		kryo.register(PlayerInput.class);
		kryo.register(PlayerInput.Event.class);
		kryo.register(int[].class);
		kryo.register(PlayerState.class);
		kryo.register(PlayerState[].class);
		kryo.register(ArrayList.class);
		kryo.register(PlayerInput[].class);
		kryo.register(String.class);
		kryo.register(Vector2.class);
		
		for(Class klass : userClasses){
			kryo.register(klass);
		}
	}
	
	public static void RegisterClass(Class klass){
		userClasses.add(klass);
	}
	
	public MessageRegistry()
	{
		callbacks = new ArrayList<MessageCallback>();
	}
	
	public synchronized int listen(Class<?> klass, MessageCallback messageCallback){
		messageCallback.type = klass;
		callbacks.add(messageCallback);
		
		return callbacks.size() - 1;
	}
	
	public synchronized void remove(int index){
		callbacks.remove(index);
	}
	
	public void invoke(Class<?> type, Connection c, Message message){
		for(MessageCallback cb : callbacks){
			if(cb.type == type){
				cb.messageReceived(c, message);
			}
		}
	}
}
