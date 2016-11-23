package com.hkamran.hoones.server;

import com.hkamran.hoones.server.servers.GameManager;

public class Main {
	public static void main(String[] args) throws Exception {
//		GameServer server = new GameServer();
//		server.start(8090);
		
		GameManager manager = new GameManager();
		manager.getRoom();
	}
}
