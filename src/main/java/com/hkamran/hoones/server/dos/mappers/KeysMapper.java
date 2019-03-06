package com.hkamran.hoones.server.dos.mappers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hkamran.hoones.server.dos.Keys;

public class KeysMapper {
	
	private static final String CYCLE = "cycle";
	private static final String PLAYER_ID = "playerId";
	private static final String DATA = "data";

	public static Keys toKeys(JSONObject json) {
		try {
			int[] keys = new int[8];
			JSONArray jKeys = json.getJSONArray(DATA);
			for (int i = 0; i < keys.length; i++) {
				keys[i] = jKeys.getInt(i);
			}
			
			int playerId = json.getInt(PLAYER_ID);
			int cycle = json.getInt(CYCLE);
			
			Keys keyStroke = new Keys(playerId, cycle, keys);
			
			return keyStroke;
		} catch (JSONException e) {
			System.out.println(json.toString(2));
			throw e;
		}
	}
	
	public static JSONObject toJSONObject(Keys keys) {
		JSONObject json = new JSONObject();
		json.put(DATA, keys.data);
		json.put(PLAYER_ID, keys.playerId);
		json.put(CYCLE, keys.cycle);
		return json;
	}
	
}
