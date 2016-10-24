package com.hkamran.hoones.server.payloads;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Keys {
	
	int[] data = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
	int playerId;
	int cycle;
	
	public Keys(Integer id, Integer cycle) {
		this.playerId = id;
		this.cycle = cycle;
	}
	
	public Keys(Integer id, Integer cycle, int[] keys) {
		this.playerId = id;
		this.cycle = cycle;
		this.data = keys;
	}

	public static Keys parseJSON(JSONObject json) {
		try {
			int[] keys = new int[8];
			JSONArray jKeys = json.getJSONArray("data");
			for (int i = 0; i < keys.length; i++) {
				keys[i] = jKeys.getInt(i);
			}
			
			int playerId = json.getInt("playerId");
			int cycle = json.getInt("cycle");
			
			Keys keyStroke = new Keys(playerId, cycle, keys);
			
			return keyStroke;
		} catch (JSONException e) {
			System.out.println(json.toString(2));
			throw e;
		}
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("data", data);
		json.put("playerId", playerId);
		json.put("cycle", cycle);
		return json;
	}
	
	public static void main(String[] args) {
		Keys controller = new Keys(1, 32);
		System.out.println(controller.toJSON().toString(2));
		System.out.println("");
		controller = Keys.parseJSON(controller.toJSON());
		System.out.println(controller.toJSON().toString(2));
	}
}
