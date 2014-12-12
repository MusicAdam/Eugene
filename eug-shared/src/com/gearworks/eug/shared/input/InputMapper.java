package com.gearworks.eug.shared.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * TODO: Rewrite this class to provide mapping from an input to a callback
 */
public class InputMapper {
	
	private HashMap<PlayerInput.Event, InputResolver> map = new HashMap<PlayerInput.Event, InputResolver>();
	
	public InputResolver put(PlayerInput.Event event, InputResolver resolver){
		map.put(event, resolver);
		return resolver;
	}
	
	public InputResolver get(PlayerInput.Event event){
		return map.get(event);
	}
	
	
}
