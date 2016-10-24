package com.hkamran.hoones.server.payloads;

import org.json.JSONArray;
import org.json.JSONObject;

public class Controller {
	
	int[] keys = new int[] {0, 0, 0, 0, 0, 0, 0, 0};

	int id;
	int cycle;
	
	public Controller(Integer id, Integer cycle) {
		this.id = id;
		this.cycle = cycle;
	}
	
	public Controller(Integer id, Integer cycle, int[] keys) {
		this.id = id;
		this.cycle = cycle;
		this.keys = keys;
	}

	public static Controller parseJSON(JSONObject json) {

		int[] keys = new int[8];
		JSONArray jKeys = json.getJSONArray("keys");
		for (int i = 0; i < keys.length; i++) {
			keys[i] = jKeys.getInt(i);
		}
		
		int playerId = json.getInt("id");
		int cycle = json.getInt("cycle");
		
		Controller keyStroke = new Controller(playerId, cycle, keys);
		
		System.out.println(keyStroke.toJSON().toString(2));
		return keyStroke;
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("keys", keys);
		json.put("id", id);
		json.put("cycle", cycle);
		return json;
	}
	
	public static void main(String[] args) {
		Controller controller = new Controller(1, 32);
		System.out.println(controller.toJSON().toString(2));
		System.out.println("");
		controller = Controller.parseJSON(controller.toJSON());
		System.out.println(controller.toJSON().toString(2));
	}
}
