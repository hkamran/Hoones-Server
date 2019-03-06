package com.hkamran.hoones.server.dos.mappers;

import org.json.JSONObject;

import com.hkamran.hoones.server.dos.Player;

public class PlayerMapper {
	
	private static final String CYCLE = "cycle";
	private static final String ID = "id";

	public static JSONObject toJSONObject(Player player) {
		JSONObject json = new JSONObject();
		json.put(ID, player.id);
		json.put(CYCLE, player.cycle);
		return json;
	}
	
	public static Player toPlayer(JSONObject json) {
		int id = json.getInt(ID);
		int cycle = json.getInt(CYCLE);
		
		Player player = new Player(id);
		player.id = id;
		player.cycle = cycle;
		
		return player;
	}
}
