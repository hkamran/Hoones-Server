package com.hkamran.hoones.server.payloads;

import javax.websocket.Session;

import org.json.JSONObject;

public class Player {
	
	public Session session;
	public int id;
	public int cycle;
	public Status status;
	public int count = 0;
	
	public static enum Status {
		READY, WAITING
	}
	
	public Player(Session session, Integer id) {
		this.session = session;
		this.id = id;
	}

	public Player(int id) {
		this.id = id;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("cycle", cycle);
		return json;
	}
	
	public static Player parseJSON(JSONObject json) {
		int id = json.getInt("id");
		int cycle = json.getInt("cycle");
		
		Player player = new Player(id);
		player.id = id;
		player.cycle = cycle;
		
		return player;
	}
}
