package com.hkamran.hoones.server.dtos;

public class Packet {
	
	public Integer type;
	public Object data;
	
	public Packet(Integer type, Object dataObject) {
		this.type = type;
		this.data = dataObject;
	}
	
	public Packet(Integer type) {
		this.type = type;
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
}
