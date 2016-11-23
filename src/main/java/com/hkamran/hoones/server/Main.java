package com.hkamran.hoones.server;

import com.hkamran.hoones.server.servers.GameManager;

public class Main {
	public static void main(String[] args) throws Exception {
//		GameServer server = new GameServer();
//		server.start(8090);
		GameManager.addRoomPorts(8091, 8092);
		GameManager.start(8090);
	}
}
