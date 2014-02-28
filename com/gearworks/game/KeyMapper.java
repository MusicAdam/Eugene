package com.gearworks.game;

import java.awt.RenderingHints.Key;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Input.Keys;

//Maps lingdx keycodes with Generic Game actions
// THIS CLASS IS LARGELY UNTESTED
public class KeyMapper {
	public enum Action{
		MoveLeft,
		MoveRight,
		Jump
	}
	
	private HashMap<Integer, Action> mappings;

	public KeyMapper() {
		mappings = new HashMap<Integer, Action>();
		//Setup default key mappings
		mappings.put(Keys.A, Action.MoveLeft);
		mappings.put(Keys.D, Action.MoveRight);
		mappings.put(Keys.SPACE, Action.Jump);
	}
	
	public boolean isMapped(int key){
		return (mappings.containsKey(key) && mappings.get(key) != null);
	}
	
	public Action getMappedAction(int keycode){
		return mappings.get(keycode);
	}
	
	public void setMapping(int keycode, Action action){
		if(mappings.containsKey(keycode)){
			mappings.remove(keycode);
		}
		
		mappings.put(keycode, action);
	}
	
	public void clear(){
		mappings.clear();
	}
	
	public void removeMapping(int keycode){
		mappings.remove(keycode);
		mappings.put(keycode, null);
	}
}
