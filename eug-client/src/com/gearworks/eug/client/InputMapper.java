package com.gearworks.eug.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * TODO: Rewrite this class to provide mapping from an input to a callback
 */
public class InputMapper extends HashMap<String, List<Integer>> {
	
	public void put(String key, Integer val){
		List<Integer> current = get(key);
		
		if(current == null){
			current = new ArrayList<Integer>();
			super.put(key, current);
		}
		
		current.add(val);
	}
	
	public String getMapping(int keycode){
		for(Map.Entry<String, List<Integer>> e : entrySet()){
			if(e.getValue().contains(keycode)){
				return (String)e.getKey();
			}
		}
		
		return null;
	}
	
	
}
