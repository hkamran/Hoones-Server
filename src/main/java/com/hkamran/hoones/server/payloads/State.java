package com.hkamran.hoones.server.payloads;

import org.json.JSONObject;

public class State {

	public int cycle;
	public int playerId;
	public JSONObject data;
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("cycle", cycle);
		json.put("playerId", playerId);
		json.put("data", data);
		
		return json;
	}
	
	public static State parseJSON(JSONObject json) {
		
		int cycle = json.getInt("cycle");
		int id = json.getInt("playerId");
		JSONObject data = json.getJSONObject("data");
		
		State state = new State();
		state.cycle = cycle;
		state.playerId = id;
		state.data = data;

		return state;
	}
	
}
