package com.hkamran.hoones.server;

import org.json.JSONObject;

import com.hkamran.hoones.server.payloads.Keys;
import com.hkamran.hoones.server.payloads.Player;
import com.hkamran.hoones.server.payloads.State;

public class Payload {

	public Type type;
	public Object data;
	
	public Payload(Type type, Object data) {
		this.type = type;
		this.data = data;
	}
	
	public Payload() {
		
	}
	
	public static enum Type {
		SERVER_GETSTATE, SERVER_PUTSTATE, SERVER_STOP, SERVER_PLAY, SERVER_WAIT,
		
		SERVER_PLAYERINFO,SERVER_PLAYERKEYS, SERVER_PLAYERCONNECTED, SERVER_PLAYERDISCONNECTED,
		
		PLAYER_KEYS, PLAYER_WAITING, PLAYER_SYNC, PLAYER_SENDSTATE
	}
	
	public static Payload STOP() {
		Payload payload = new Payload();
		payload.type = Type.SERVER_STOP;
		return payload;
	}
	
	public static Payload PLAY() {
		Payload payload = new Payload();
		payload.type = Type.SERVER_PLAY;
		return payload;
	}
	
	public static Payload WAIT() {
		Payload payload = new Payload();
		payload.type = Type.SERVER_WAIT;
		return payload;
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("type", type.toString());
		
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
		
		Type type = Type.valueOf(json.getString("type"));
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
