package com.gearworks.eug.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gearworks.eug.shared.events.EntityEventListener;
import com.gearworks.eug.shared.events.PlayerEventListener;
import com.gearworks.eug.shared.exceptions.*;
import com.gearworks.eug.shared.Eug.NotImplementedException;
import com.gearworks.eug.shared.exceptions.EntityNotRegisteredException;
import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.state.AbstractEntityState;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.CircularBuffer;

public class World {
	String name;
	ArrayList<EntityEventListener>			entityEventListeners = new ArrayList<EntityEventListener>();
	HashMap<Short, NetworkedEntity> 		entityMap = new HashMap<Short, NetworkedEntity>();
	ArrayList<Player> 		 				players = new ArrayList<Player>();
	CircularBuffer<Snapshot>				history;
	Snapshot 								latestSnapshot;
	int										instanceId;
	short									lastEntityID; //The last entity to be added to the world.
	
	//Entity queues
	ConcurrentLinkedQueue<NetworkedEntity>			entitySpawnQueue = new ConcurrentLinkedQueue<NetworkedEntity>();
	ConcurrentLinkedQueue<NetworkedEntity>			entityDeleteQueue = new ConcurrentLinkedQueue<NetworkedEntity>();
	//Player queues
	ConcurrentLinkedQueue<Player>			playerAddQueue = new ConcurrentLinkedQueue<Player>();
	ConcurrentLinkedQueue<Player>			playerDeleteQueue = new ConcurrentLinkedQueue<Player>();
	
	
	//Entity locks
	Object 									entitySpawnLock = new Object();
	Object 									entityDeleteLock = new Object();
	//Player locks
	Object 									playerAddLock = new Object();
	Object									playerDeleteLock = new Object();
	//History lock
	Object									historyLock = new Object();
	
	private boolean simulator;
	
	public World(String name, int instanceId){
		this(name, instanceId, false);
	}
	
	public World(String name, int instanceId, boolean sim){
		if(!sim)
			history = new CircularBuffer<Snapshot>(SharedVars.HISTORY_SIZE);
		this.name = name;
		simulator = sim;
		lastEntityID = -1;
	}
	
	public void update(float step){
		
		//
		//Process new players
		synchronized(playerAddLock){
			while(!playerAddQueue.isEmpty())
				addPlayer(playerAddQueue.poll());			
		}
		
		//
		//Process deleted players
		synchronized(playerDeleteLock){
			while(!playerDeleteQueue.isEmpty())
				addPlayer(playerDeleteQueue.poll());			
		}
		
		//
		//Process new entities
		synchronized(entitySpawnLock){
			while(!entitySpawnQueue.isEmpty())
				spawn(entitySpawnQueue.poll());
		}
		
		//
		//Process deleted entities
		synchronized(entityDeleteLock){
			while(!entityDeleteQueue.isEmpty()){
				destroy(entityDeleteQueue.poll());
			}
		}
				
		if(!simulator){			
			for(Player player : Eug.GetPlayers()){
				for(PlayerInput input : player.getInputs()){
					if(!input.isSaved()){
						latestSnapshot.addInput(input);
					}
				}
				
				
				for(NetworkedEntity ent : player.getEntities()){
					ent.update();
				}
			}
			
			synchronized(historyLock){
				history.push(generateSnapshot());
				latestSnapshot = history.peekTail();
			}
		}
	}
	
	public void render(){
		if(simulator) return;
	}
	
	public void snapToSnapshot(Snapshot snapshot){
		synchronizeSnapshot(snapshot);
		
		for(AbstractEntityState state : snapshot.getEntityStates()){
			NetworkedEntity ent = getEntity(state.getId());
			ent.snapToState(state);
		}
	}
	
	/*
	 * Sets the world to exactly reflect the given snapshot
	 * Players will be created/destroyed
	 * New entities will be created
	 * Entities that no longer exist will be destroyed
	 */
	public void synchronizeSnapshot(Snapshot snapshot){
		HashSet<Integer> serverPlayerSet = new HashSet<Integer>();
		HashSet<Integer> localPlayerSet = new HashSet<Integer>();
		HashSet<Short> serverEntitySet = new HashSet<Short>();
		HashSet<Short> localEntitySet = new HashSet<Short>();
		
		if(isSynchronized(snapshot, serverPlayerSet, localPlayerSet, serverEntitySet, localEntitySet)) return;
		
		//Get the serverPLayers - localPlayers to find which players have connected
		HashSet<Integer> newPlayers = new HashSet<Integer>(serverPlayerSet);
		newPlayers.removeAll(localPlayerSet);
		
		//Get localPlayers - serverPlayer to determine which players have disconnected
		HashSet<Integer> deletedPlayers = new HashSet<Integer>(localPlayerSet);
		deletedPlayers.removeAll(serverPlayerSet);
		
		for(int newPlayerId : newPlayers){
			//Find the player's state according to the server and build the player using that
			PlayerState newPlayerState = null;
			for(int i = 0; i < snapshot.getPlayers().length; i++){
				if(snapshot.getPlayers()[i].getId() == newPlayerId){
					newPlayerState = snapshot.getPlayers()[i];
					break;
				}
			}
			
			Player pl = new Player(newPlayerState);
			addPlayer(pl);
		}
		
		for(int deletedPlayerId : deletedPlayers){
			Player pl = Eug.FindPlayerById(deletedPlayerId);

			if(pl  != null){
				pl.dispose();
				removePlayer(pl);
			}
		}

		//Get the serverPLayers - localPlayers to find which players have connected
		HashSet<Short> newEntityIds = new HashSet<Short>(serverEntitySet);
		newEntityIds.removeAll(localEntitySet);
		
		//Get localPlayers - serverPlayer to determine which players have disconnected
		HashSet<Short> deletedEntityIds = new HashSet<Short>(localEntitySet);
		deletedEntityIds.removeAll(serverEntitySet);

		for(Short id : newEntityIds){
			AbstractEntityState state = snapshot.getEntityState(id);
			try {
				if(EntityManager.Build(getPlayer(state.getPlayerId()), state.getType(), this, state)==null)
					throw new EntityBuildException("Unable to build entity, entity returned null");
			} catch (EntityNotRegisteredException | EntityBuildException e) {
				e.printStackTrace();
			}
		}
		
		for(Short id : deletedEntityIds){
			NetworkedEntity ent = getEntity(id);

			if(ent != null){
				destroy(ent);
			}
		}
	}
	
	//Checks if the same players/entities exist in the world that exist in the snapshot.
	public boolean isSynchronized(Snapshot snapshot, HashSet<Integer> serverPlayerSet, HashSet<Integer> localPlayerSet, HashSet<Short> serverEntitySet, HashSet<Short> localEntitySet){
		if(serverPlayerSet == null)
			serverPlayerSet = new HashSet<Integer>();
		if(localPlayerSet == null)
			localPlayerSet = new HashSet<Integer>();
		if(serverEntitySet == null)
			serverEntitySet = new HashSet<Short>();
		if(localEntitySet == null)
			localEntitySet = new HashSet<Short>();
		
		//Construct sets
		for(PlayerState pl : snapshot.getPlayers())
			serverPlayerSet.add(pl.getId());
		for(Player pl : players)
			localPlayerSet.add(pl.getId());
		
		//Construct sets
		for(AbstractEntityState entState : snapshot.getEntityStates())
			serverEntitySet.add(entState.getId());
		for(NetworkedEntity ent : entityMap.values())
			localEntitySet.add(ent.getId());
		
		HashSet<Integer> unsyncedPlayers = new HashSet<Integer>(serverPlayerSet);
		HashSet<Short> unsyncedEntities = new HashSet<Short>(serverEntitySet);
		
		unsyncedPlayers.removeAll(localPlayerSet);
		unsyncedEntities.removeAll(localEntitySet);
		
		return (unsyncedPlayers.size() == 0 && unsyncedEntities.size() == 0);
	}
	
	public boolean isSynchronized(Snapshot snapshot){
		return isSynchronized(snapshot, null, null, null, null);
	}
	
	public Snapshot findPastSnapshot(long time, long epsilon){
		synchronized(historyLock){
			if(history.isEmpty()) return null;
			
			int inc = 0;
			
			while(inc < history.count()){
				if(history.peek(inc).getTimestamp() + epsilon > time && history.peek(inc).getTimestamp() - epsilon < time){
					return history.peek(inc);
				}
				inc++;
			}
			
			return null;
		}
	}
	
	public Snapshot findPastSnapshot(long time){
		return findPastSnapshot(time, SharedVars.TIMESTAMP_EPSILON);
	}
	
	public NetworkedEntity spawn(NetworkedEntity ent){
		if(Eug.OnMainThread() || simulator){
			synchronized(entitySpawnLock){
				if(ent.getPlayer().isValid()){
					if(simulator){
						Debug.println("[Simulator:spawn] entity " + ent.getId());
					}else{
						Debug.println("[World:spawn] entity " + ent.getId());						
					}
					ent.spawn(this);
					entityMap.put(ent.getId(), ent);
					
					for(EntityEventListener listener : entityEventListeners){
						listener.onCreate(ent);
					}
					
					return ent;
				}
			}
		}else{
			synchronized(entitySpawnLock){
				entitySpawnQueue.add(ent);
			}
		}
		return null;
	}
	
	public void destroy(NetworkedEntity ent){
		if(Eug.OnMainThread() || simulator){
			synchronized(entityDeleteLock){
				if(!entityMap.containsKey(ent.getId())) return; //TODO: Is this necessary?
				
				entityMap.remove(ent.getId());	
				ent.dispose();	
		
				for(EntityEventListener listener : entityEventListeners){
					listener.onDestroy(ent);
				}
				
				Debug.println("[World:destroy] Entity " + ent.getId() + " deleted");
			}
		}else{
			synchronized(entityDeleteLock){
				entityDeleteQueue.add(ent);
				Debug.println("[World:destroy] Entity " + ent.getId() + " queued for deletion");
			}
		}
	}
	
	public Player addPlayer(Player pl){
		if(Eug.OnMainThread() || simulator){
			synchronized(playerAddLock){
				if(!players.contains(pl)){
					players.add(pl);
					
					for(PlayerEventListener listener : Eug.GetPlayerEventListeners()){
						listener.AddedToWorld(pl);
					}
					
					Debug.println("[World:addPlayer] Player " + pl.getId() + " added");
				}
				
				return pl;
			}
		}else{
			synchronized(playerAddLock){
				playerAddQueue.add(pl);
			}
		}
		
		return null;
	}
	
	public boolean removePlayer(Player pl){
		if(Eug.OnMainThread() || simulator){
			synchronized(playerDeleteLock){
				if(players.remove(pl)){
					
					for(PlayerEventListener listener : Eug.GetPlayerEventListeners()){
						listener.RemovedFromWorld(pl);
					}
					
					Debug.println("[World:removePlayer] Player " + pl.getId() + " removed");
					return true;
				}
				
				return false;
			}
		}else{
			synchronized(playerDeleteLock){
				playerDeleteQueue.add(pl);
			}
		}
		
		return false;
	}
	
	public Snapshot generateSnapshot() {
		PlayerState[] playerStates = new PlayerState[players.size()];		
		ArrayList<PlayerInput> inputsList = new ArrayList<PlayerInput>();
		
		int i = 0;
		for(Player pl : players){
			playerStates[i] = pl.getState();
			i++;
			
			for(PlayerInput input : pl.getInputs()){
				if(!input.isSaved()){
					input.setSaved(true);
					inputsList.add(input);
				}
			}
		}
		
		PlayerInput[] inputsArray = new PlayerInput[inputsList.size()];
		inputsList.toArray(inputsArray);
		
		return new Snapshot(instanceId, playerStates, getEntityStates(), inputsArray);
	}
	
	public AbstractEntityState[] getEntityStates() {
		AbstractEntityState[] states = new AbstractEntityState[entityMap.entrySet().size()];
		Iterator entIt = entityMap.entrySet().iterator();
		int i = 0;
		while(entIt.hasNext())
		{ 
			Map.Entry<Integer, NetworkedEntity> pairs = (Map.Entry<Integer, NetworkedEntity>)entIt.next();
			pairs.getValue().update();
			try {
				states[i] = pairs.getValue().getState();
			} catch (NotImplementedException e) {
				e.printStackTrace();
			}
			i++;
		}
		
		return states;
	}
	
	public NetworkedEntity getEntity(int id){
		return entityMap.get(id);
	}

	public ArrayList<Player> getPlayers() {
		return players;
	}

	public int countEntities() {
		return entityMap.size();
	}

	public Collection<NetworkedEntity> getEntities() {
		return entityMap.values();
	}
	
	public HashMap<Short, NetworkedEntity> getEntityMap(){
		return entityMap;
	}

	public int countPlayers() {
		return players.size();
	}
	
	public Player getPlayer(int id){
		for(int i = 0; i < players.size();i++){
			if(players.get(i).getId() == id)
				return players.get(i);
		}
		
		return null;
	}
	
	public CircularBuffer<Snapshot> getHistory(){ return history; }
	
	public Snapshot getLatestSnapshot(){ return latestSnapshot; }
	public void setInstanceId(int id){ instanceId = id; }

	public EntityEventListener addEntityListener(EntityEventListener entityEventListener) {
		entityEventListeners.add(entityEventListener);
		return entityEventListener;
	}

	public void removeEntityListener(EntityEventListener entityEventListener) {
		entityEventListeners.remove(entityEventListener);		
	}
	
	public PlayerState[] getPlayerStates(){
		PlayerState[] playerStates = new PlayerState[players.size()];
		
		for(int i = 0; i < players.size(); i++){
			playerStates[i] = players.get(i).getState();
		}
		
		return playerStates;
	}

	public boolean isSimulation() {
		return simulator;
	}
	
	public String getName(){ return name; }

	public Snapshot pruneHistory(long timestamp) {
		Snapshot last = null; 
		while(!history.isEmpty() && history.peek().getTimestamp() < timestamp){
			last = history.pop();
		}

		return (history.peek() == null) ? last : history.peek();
	}
	
	public short nextEntityID()
	{
		short id = (short) (lastEntityID + 1);
		if(id < 0)
			id = 0;
		
		while(getEntity(id) != null)
			id++;
		
		return id;
	}
}
