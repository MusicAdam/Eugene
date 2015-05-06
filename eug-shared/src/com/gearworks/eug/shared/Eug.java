package com.gearworks.eug.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryonet.Connection;
import com.gearworks.eug.shared.events.PlayerEventListener;
import com.gearworks.eug.shared.input.InputMapper;
import com.gearworks.eug.shared.state.StateManager;

/*
 * Provides a base class from which shared classes can call methods that should be implemented on both the client and server, but may be implemented differently
 */
public class Eug {
	private static Eug singleton;
	private static String mainThreadName; //This is the name of the thread that initialize is called on. Should be called by the user on the main thread.
	
	public Object playerLock = new Object();
	public Object messageLock = new Object();
	public Object entityLock = new Object();
	
	private ArrayList<PlayerEventListener>	playerEventListeners = new ArrayList<PlayerEventListener>();
	private static InputMapper inputMapper;
	
	public class NotImplementedException extends Exception
	{
		private static final long serialVersionUID = 1L;
	}
	
	public static Eug Get()
	{
		return singleton;
	}
	
	public static Eug Set(Eug e)
	{
		singleton = e;
		return Get();
	}
	
	public static void Initialize(){
		mainThreadName = Thread.currentThread().getName();
		
		EntityManager.Register(NetworkedEntity.NETWORKED_ENTITIY, NetworkedEntity.class);
	}
	
	public static boolean OnMainThread(){
		return mainThreadName.equals(Thread.currentThread().getName());
	}

	protected World getWorld() throws NotImplementedException{ throw new NotImplementedException(); }
	public static World GetWorld()
	{
		try {
			return Get().getWorld();
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected StateManager getStateManager() throws NotImplementedException{ throw new NotImplementedException(); }
	public static StateManager GetStateManager()
	{
		try {
			return Get().getStateManager();
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}


	protected void destroyEntity(NetworkedEntity ent) throws NotImplementedException{ throw new NotImplementedException(); }
	public static void DestroyEntity(NetworkedEntity ent)
	{
		try {
			Get().destroyEntity(ent);
		} catch (NotImplementedException e) {
			e.printStackTrace();
		}
	}

	protected NetworkedEntity spawnEntity(NetworkedEntity ent) throws NotImplementedException{ throw new NotImplementedException(); }
	public static NetworkedEntity SpawnEntity(NetworkedEntity ent)
	{
		try {
			return Get().spawnEntity(ent);
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}
	

	protected Map<Short, NetworkedEntity> getEntities() throws NotImplementedException{ throw new NotImplementedException(); }
	public static Map<Short, NetworkedEntity> GetEntities()
	{
		try {
			return Get().getEntities();
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected Player findPlayerById(int id) throws NotImplementedException{ throw new NotImplementedException(); }
	public static Player FindPlayerById(int id){
		try {
			return Get().findPlayerById(id);
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected NetworkedEntity findEntityById(short id) throws NotImplementedException{ throw new NotImplementedException(); }
	public static NetworkedEntity FindEntityById(short id) {
		try {
			return Get().findEntityById(id);
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Returns the connection by the given connection id if it exists.
	 */
	protected Connection getConnectionById(int id) throws NotImplementedException{ throw new NotImplementedException(); }
	public static Connection GetConnectionById(int id){ 
		try {
			return Get().getConnectionById(id);
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected boolean entityExists(short id) throws NotImplementedException{ throw new NotImplementedException(); }
	public static boolean EntityExists(short id) {
		try {
			return Get().entityExists(id);
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected List<Player> getPlayers() throws NotImplementedException{ throw new NotImplementedException(); }
	public static List<Player> GetPlayers(){
		try {
			return Get().getPlayers();
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected NetworkedEntity spawn(NetworkedEntity ent) throws NotImplementedException{ throw new NotImplementedException(); }
	public static NetworkedEntity Spawn(NetworkedEntity ent){
		try {
			return Get().spawn(ent);
		} catch (NotImplementedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected void destroy(NetworkedEntity ent) throws NotImplementedException{ throw new NotImplementedException(); }
	public static void Destroy(NetworkedEntity ent){
		try {
			Get().destroy(ent);
		} catch (NotImplementedException e) {
			e.printStackTrace();
		}
	}
	
	public static PlayerEventListener AddPlayerListener(PlayerEventListener playerEventListener){
		Get().playerEventListeners.add(playerEventListener);
		return playerEventListener;
	}
	
	public static void RemovePlayerListener(PlayerEventListener playerEventListener){
		Get().playerEventListeners.remove(playerEventListener);
	}
	
	public static ArrayList<PlayerEventListener> GetPlayerEventListeners(){
		return Get().playerEventListeners;
	}
	
	public static void SetInputMapper(InputMapper im){
		inputMapper = im;
	}
	
	public static InputMapper GetInputMapper(){ return inputMapper; }
	
	public void create(){}
	public void update(float step){}
	public void dispose(){}
}
