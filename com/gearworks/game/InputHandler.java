package com.gearworks.game;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.InputProcessor;

public class InputHandler implements InputProcessor {
	public KeyMapper keyMapper;
	public HashMap<KeyMapper.Action, Boolean> actionMap;
	
	public InputHandler(){
		keyMapper = new KeyMapper();
		actionMap = new HashMap<KeyMapper.Action, Boolean>();
	}
	
	public void toggleAction(KeyMapper.Action action, boolean bool){
		if(actionMap.containsKey(action)){
			actionMap.remove(action);
		}
		
		actionMap.put(action, bool);
	}
	
	public boolean isAction(KeyMapper.Action action){
		if(actionMap.containsKey(action)){
			return actionMap.get(action);
		}
		
		return false;
	}
	
	@Override
	public boolean keyDown(int key) {
		if(keyMapper.isMapped(key)){
			toggleAction(keyMapper.getMappedAction(key), true);
		}
		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyUp(int key) {
		if(keyMapper.isMapped(key)){
			toggleAction(keyMapper.getMappedAction(key), false);
		}
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

}
