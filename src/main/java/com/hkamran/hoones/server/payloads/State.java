package com.hkamran.hoones.server.payloads;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class State {

	public int cycle;
	public int id;
	public Status status;
	public Map<String, Object> state = new HashMap<String, Object>(); 
	
	public static enum Status {
		GET, STATUS, UPDATE, WAITING
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("cycle", cycle);
		json.put("id", id);
		json.put("status", status.toString());
		
		JSONObject stateJSON = new JSONObject();
		for (String key : state.keySet()) {
			Object result = state.get(key);
			if (result instanceof int[]) {
				state.put(key, (int[]) result);
			} else if (result instanceof Integer) {
				state.put(key, (int) result);
			} else {
				throw new RuntimeException("Unknown result object");
			}
			
		}
		
		json.put("state", stateJSON);
		
		return json;
	}
	
	public static State parseJSON(JSONObject json) {
		
		int cycle = json.getInt("cycle");
		int id = json.getInt("int");
		Status status = Status.valueOf(json.getString("status"));
		Map<String, Object> stateMap = new HashMap<String, Object>();
		
		JSONObject stateJson = json.getJSONObject("state");
		for (Object key : stateJson.keySet()) {
			Object result = stateJson.get((String) key);
			if (result instanceof int[]) {
				stateMap.put((String) key, (int[]) result);
			} else {
				stateMap.put((String) key, (int) result);
			}
		}
		
		State state = new State();
		state.cycle = cycle;
		state.id = id;
		state.status = status;
		state.state = stateMap;

		return state;
	}
	
}
