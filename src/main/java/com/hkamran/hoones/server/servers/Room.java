package com.hkamran.hoones.server.servers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

import com.hkamran.hoones.server.payloads.Player;

public class Room {
	
	private final static Logger log = LogManager.getLogger(Room.class);
	
	public static enum Status { SYNCING, WAITING, PLAYING };
	public static final int SIZE = 2;
	
	//Server variables
	private Server server;
	public Integer id;
	public Integer port;
	
	//Room variables
	public Player[] seats = new Player[SIZE];
	public Map<Session, Player> players = new HashMap<Session, Player>();
	public Status status = Status.PLAYING;
	public long lastMessage = System.currentTimeMillis();
	
	public Room(int port) throws Exception {
		this.server = GameServer.create(port);
		this.id = ((Integer) this.server.hashCode());
		this.port = port;
		log.info("Create game room " + id + " on " + port);
	}
	
	public boolean hasEmptySeat() {
		int result = getEmptySeat();
		if (result == -1) {
			return false;
		}
		return true;
	}
	
	public int getEmptySeat() {
		for (int i = 0; i < seats.length; i++) {
			if (seats[i] == null) {
				return i;
			}
		}
		return -1;
	}
	
	public Player getPlayer(Session session) {
		return players.get(session);
	}
	
	public Player takeSeat(int index, Session session) {
		Player player = new Player(session, index + 1);
		seats[index] = player;
		players.put(session, player);
		log.info(String.format("Player %S connected on game %S on port %S", player.id, id, port));

		return player;
	}

	public Player getHost() {
		for (int i = 0; i < seats.length; i++) {
			Player current = seats[i];
			if (current != null) {
				return current;
			}
		}
		return null;
	}
	
	public void leaveSeat(Session session) {

		for (int i = 0; i < seats.length; i++) {
			Player player = seats[i];
			if (player.session == session) {
				seats[i] = null;
				log.info(String.format("Player %S disconnected on game %S on port %S", player.id, id, port));
				break;
			}
		}
		
		players.remove(session);
	}
	
	
	public List<Player> getPlayers() {
		List<Player> players = new ArrayList<Player>();
		for (int i = 0; i < seats.length; i++) {
			if (seats[i] == null) {
				continue;
			}
			players.add(seats[i]);
		}
		return players;
	}
	
	public void destroy() {
		log.info("Destroying room " + id + " on " + port);
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			server.destroy();
		}

	}
	
}