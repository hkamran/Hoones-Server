package com.hkamran.hoones.server;

import org.json.JSONObject;

import com.hkamran.hoones.server.payloads.Keys;
import com.hkamran.hoones.server.payloads.Player;
import com.hkamran.hoones.server.payloads.State;

public class Payload {

	public Integer type;
	public Object data;
	
	public Payload(Integer type, Object data) {
		this.type = type;
		this.data = data;
	}
	
	public Payload() {
		
	}
	
	public static class Type {
		public static int SERVER_GETSTATE = 0;
		public static int SERVER_PUTSTATE = 1;
		public static int SERVER_STOP = 2;
		public static int SERVER_PLAY = 3;
		public static int SERVER_DESTROYED = 13;
		public static int SERVER_FULL = 14;
		
		public static int SERVER_PLAYERINFO = 5;
		public static int SERVER_PLAYERKEYS = 6;
		public static int SERVER_PLAYERCONNECTED = 7; 
		public static int SERVER_PLAYERDISCONNECTED = 8;
		
		public static int PLAYER_KEYS = 9;
		public static int PLAYER_WAITING = 10;
		public static int PLAYER_SYNC = 11;
		public static int PLAYER_SENDSTATE = 12;
	}
	
	public static Payload STOP() {
		Payload payload = new Payload();
		payload.type = Type.SERVER_STOP;
		return payload;
	}
	
	public static Payload FULL() {
		Payload payload = new Payload();
		payload.type = Type.SERVER_FULL;
		return payload;		
	}
	
	public static Payload PLAY() {
		Payload payload = new Payload();
		payload.type = Type.SERVER_PLAY;
		return payload;
	}
	
	public static Payload DESTROYED() {
		Payload payload = new Payload();
		payload.type = Type.SERVER_DESTROYED;
		return payload;
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("type", type);
		
		if (data instanceof Keys) {
			json.put("data", ((Keys) data).toJSON());
		} else if (data instanceof Player) {
			json.put("data", ((Player) data).toJSON());
		} else if (data instanceof State) {
			json.put("data", ((State) data).toJSON());
		}
		
		return json;
	}
	
	public static Payload parseJSON(String str) {
		JSONObject json = new JSONObject(str);
		
		Integer type = json.getInt("type");
		Object result = null;
		
		
		JSONObject jsonObj = json.getJSONObject("data");
		if (type == Type.PLAYER_KEYS) {
			Keys keyStroke = Keys.parseJSON(jsonObj);
			result = keyStroke;
		} else if (type == Type.PLAYER_SENDSTATE) {
			State state = State.parseJSON(jsonObj);
			result = state;
		} else if (type == Type.PLAYER_WAITING) {
		} else {
			throw new RuntimeException("Recieved unknown payload type " + type);
		}
		
		Payload payload = new Payload();
		payload.type = type;
		payload.data = result;
		
		return payload;
	}

}
