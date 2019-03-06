package com.hkamran.hoones.server.dos;

import javax.websocket.Session;

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


}
