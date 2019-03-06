package com.hkamran.hoones.server.dos.mappers;

import org.json.JSONObject;

import com.hkamran.hoones.server.dos.State;

public class StateMapper {
	
	private static final String DATA = "data";
	private static final String PLAYER_ID = "playerId";
	private static final String CYCLE = "cycle";

	public static JSONObject toJSONObject(State state) {
		JSONObject json = new JSONObject();
		json.put(CYCLE, state.cycle);
		json.put(PLAYER_ID, state.playerId);
		json.put(DATA, state.data);
		return json;
	}
	
	public static State toState(JSONObject json) {
		
		int cycle = json.getInt(CYCLE);
		int id = json.getInt(PLAYER_ID);
		JSONObject data = json.getJSONObject(DATA);
		
		State state = new State();
		state.cycle = cycle;
		state.playerId = id;
		state.data = data;

		return state;
	}
}
