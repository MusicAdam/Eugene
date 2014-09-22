package com.gearworks.eug.client.state;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Transform;
import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Entity;
import com.gearworks.eug.shared.EntityEventListener;
import com.gearworks.eug.shared.EntityFactory;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.Utils;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityUpdateException;
import com.gearworks.eug.shared.messages.AssignInstanceMessage;
import com.gearworks.eug.shared.messages.InitializeSceneMessage;
import com.gearworks.eug.shared.messages.Message;
import com.gearworks.eug.shared.messages.MessageCallback;
import com.gearworks.eug.shared.messages.UpdateMessage;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.state.State;


/*
 * Encapsulates actual gameplay logic. 
 *  - Handles instance initialization
 *  - Handles scene initialization
 *  - Handles server updates
 */
public class GameState implements State {
	public static float POSITION_SMOOTHING_MAXIMUM = 1.0f; //2 meters
	public static float POSITION_SMOOTHING_RATIO = .01f; //10th of the total distance (will be scaled by latency)
	public static float POSITION_SMOOTHING_MINIMUM = 0; //Minimum delta which will be smoothed
	public static float ROTATION_SMOOTHING_MAXIMUM = Utils.degToRad(40);
	public static float ROTATION_SMOOTHING_RATIO = .01f;
	public static float ROTATION_SMOOTHING_MINIMUM = .1f; //Minimum delta which will be smoothed
	
	private int assignInstanceMessageIndex = -1;
	private int initializeSceneMessageIndex = -1;
	private int serverUpdateMessageIndex = -1;
	private EntityEventListener entityEventListener;
	private long latency;

	@Override
	public boolean canEnterState() {
		return EugClient.GetPlayer() != null && EugClient.GetPlayer().isConnected();
	}

	@Override
	public void onEnter() {
		Debug.println("[GameState: onEnter()]");
		
		//Register assigninstancemessage to wait for our instance to be assigned.
		assignInstanceMessageIndex = EugClient.GetMessageRegistry().register(AssignInstanceMessage.class, new MessageCallback(){
			@Override
			public void messageReceived(Connection c, Message msg){
				AssignInstanceMessage aMsg = (AssignInstanceMessage)msg;
				EugClient.SetInstance(aMsg.getInstanceId());
			}
		});		
		
		//Initialize entity listeners
		entityEventListener = EntityFactory.AddListener(new EntityEventListener(){
			@Override
			public void onCreate(Entity ent){
				if(ent instanceof DiskEntity){
					if(ent.getPlayer() == EugClient.GetPlayer()){
						EugClient.GetPlayer().setDisk((DiskEntity)ent);
					}
				}
			}
		});
	}

	@Override
	public boolean canExitState() {
		return true;
	}

	@Override
	public void onExit() {
		//Cleanup all our callbacks/listeners
		EntityFactory.RemoveListener(entityEventListener);
		
		if(assignInstanceMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(assignInstanceMessageIndex);
		if(initializeSceneMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(initializeSceneMessageIndex);
		if(serverUpdateMessageIndex != -1)
			EugClient.GetMessageRegistry().remove(serverUpdateMessageIndex);
		
	}

	@Override
	public void render() {
		if(EugClient.GetPlayer().isValid()){ //Render entities
			for(Entity e : EugClient.GetEntities())
			{ 
				e.render(EugClient.GetSpriteBatch(), EugClient.GetShapeRenderer());
			}
		}
	}

	@Override
	public void update() {
		if(EugClient.GetPlayer().isValid()){ //Update entities
			//Remove init scene message and registry update message if it still needs to be done.
			if(initializeSceneMessageIndex != -1){
				EugClient.GetMessageRegistry().remove(initializeSceneMessageIndex);
				initializeSceneMessageIndex = -1;
				
				final GameState thisRef = this; 
				serverUpdateMessageIndex = 	
						EugClient.GetMessageRegistry().register(UpdateMessage.class, new MessageCallback(){
							@Override
							public void messageReceived(Connection c, Message msg){
								thisRef.serverUpdate((UpdateMessage)msg);
							}
						});
			}
			
			for(Entity e : EugClient.GetEntities())
			{ 
				e.update();
			}
		}else if(EugClient.GetPlayer().isInstanceValid() && !EugClient.GetPlayer().isInitialized()){
			//Remove the assigninstancemessage callback if it still exists & create sceneinitialize listener
			if(assignInstanceMessageIndex != -1){
				EugClient.GetMessageRegistry().remove(assignInstanceMessageIndex);
				assignInstanceMessageIndex = -1;
				
				final GameState thisRef = this; 
				initializeSceneMessageIndex = EugClient.GetMessageRegistry().register(InitializeSceneMessage.class, new MessageCallback(){
					@Override
					public void messageReceived(Connection c, Message msg){
						thisRef.initializeScene(c, (InitializeSceneMessage)msg);
					}
				});
			}
		}
	}
	
	protected void initializeScene(Connection c, InitializeSceneMessage msg) {
		if(!EugClient.GetPlayer().isInstanceValid()) return;
		if(EugClient.GetPlayer().isInitialized()){
			//If for some reason we are still getting this message after we have been initialized and we are initialize, let the server know
			InitializeSceneMessage ack = new InitializeSceneMessage(EugClient.GetPlayer().getInstanceId(), null);
			EugClient.GetPlayer().getConnection().sendUDP(ack);
			Debug.println("[GameState:initializeScene] Redundant Scene initialized response sent.");	
			return;
		}
		
		EugClient.UpdatePlayers(msg.getSnapshot().getPlayerIds(), msg.getSnapshot().getDisconnectedPlayers());
		
		for(EntityState state : msg.getSnapshot().getEntityStates()){
			try {
				EntityFactory.BuildFromState(state);
				/*
				 * TODO: Handle these exceptions in a friendlier way. (Disconnect player and return to main menu? Retry?)
				 */
			} catch (EntityBuildException e) {
				e.printStackTrace();
			} catch (EntityUpdateException e) {
				e.printStackTrace();
			}
		}
		
		EugClient.GetPlayer().setInitialized(true);
		InitializeSceneMessage ack = new InitializeSceneMessage(EugClient.GetPlayer().getInstanceId(), null);
		EugClient.GetPlayer().getConnection().sendUDP(ack);
		
		Debug.println("[EugClient:initializeScene] Scene initialized and response sent.");
	}
	
	protected void serverUpdate(UpdateMessage msg) {
		if(!EugClient.GetPlayer().isValid()) return;
		
		latency = (Utils.generateTimeStamp() - msg.getSnapshot().getTimestamp());
		
		EugClient.UpdatePlayers(msg.getSnapshot().getPlayerIds(), msg.getSnapshot().getDisconnectedPlayers());
		
		//Update entities
		for(EntityState state : msg.getSnapshot().getEntityStates()){
			try {
				Entity ent;
				if((ent = Eug.FindEntityById(state.getId())) != null){
					//Only apply smoothing to other entities (not owned by us, our latency will be handled by the client side prediction)
					if(ent.getPlayer() != EugClient.GetPlayer()){
						Transform smoothTransform = state.getBodyState().getTransform();
						float deltaDist = state.getBodyState().getTransform().getPosition().cpy().sub(ent.body().getPosition()).len();
						//Smooth out the position and rotation values to make up for network lag or just snap to if the distance is too great
						if(deltaDist < POSITION_SMOOTHING_MAXIMUM && deltaDist > POSITION_SMOOTHING_MINIMUM){
							Vector2 smoothDeltaPos = smoothTransform.getPosition().cpy().sub(ent.body().getPosition()).scl(POSITION_SMOOTHING_RATIO * latency);
							smoothTransform.setPosition(ent.body().getPosition().cpy().add(smoothDeltaPos));
						}
						
						float angleDelta = state.getBodyState().getTransform().getRotation() - ent.body().getAngle();
						if(angleDelta < ROTATION_SMOOTHING_MAXIMUM && angleDelta > ROTATION_SMOOTHING_MINIMUM){
							float smoothDeltaRot   = smoothTransform.getRotation() * ROTATION_SMOOTHING_RATIO * latency;
							smoothTransform.setRotation(ent.body().getAngle() + smoothDeltaRot);
						}
						
						state.getBodyState().setTransform(smoothTransform);
					}
					EntityFactory.UpdateToState(state);
				}else{
					EntityFactory.BuildFromState(state);					
				}
				/*
				 * TODO: Handle these exceptions in a friendlier way. (Disconnect player and return to main menu? Retry?)
				 */
			} catch (EntityUpdateException e) {
				e.printStackTrace();
			} catch (EntityBuildException e){
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public int getId() {
		return 1;
	}

}
