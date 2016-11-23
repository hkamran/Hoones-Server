package com.hkamran.hoones.server.servers;


import java.util.ArrayList;
import java.util.List;

public class GameManager {

	private static final int MAXSIZE = 2;
	private static final long MAXAGE = 10000;
	List<GameServer> servers = new ArrayList<GameServer>();
	List<Integer> ports = new ArrayList<Integer>();
	
	public GameManager() {
		ports.add(8091);
		ports.add(8090);
	}
	
	public int getRoom() {
		if (servers.size() == MAXSIZE) {
			return -1;
		}
		
		clearOldServers();
		
		int port = ports.remove(ports.size() - 1);
		System.out.println("select " + port);
		try {
			GameServer server = new GameServer();
			server.start(port);
		} catch (Exception e) {
			ports.add(port);
			return -1;
		}
	
		return port;
	}

	private void clearOldServers() {
		//Remove old game servers
		List<GameServer> oldServers = new ArrayList<GameServer>();
		long currentMillis = System.currentTimeMillis();
		for (int i = 0; i < servers.size(); i++) {
			GameServer server = servers.get(i);
			if (currentMillis - server.lastMessage > MAXAGE) {
				oldServers.add(server);
			}
		}
		
		for (GameServer server : oldServers) {
			servers.remove(server);
			ports.add(server.port);
		}
	}
}
