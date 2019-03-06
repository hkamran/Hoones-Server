package com.hkamran.hoones.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.json.JSONException;
import org.json.JSONObject;

import com.hkamran.hoones.server.dos.Player;
import com.hkamran.hoones.server.dtos.factories.PayloadFactory;
import com.hkamran.hoones.server.dtos.mappers.PayloadMapper;

public class Room {

	private final static Logger LOGGER = LogManager.getLogger(Room.class);
	
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
		players = new HashMap<Session, Player>();
		this.server = RoomSocket.create(port);
		this.id = ((Integer) this.server.hashCode());
		this.port = port;
		LOGGER.info("Create game room " + id + " on " + port);
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
		LOGGER.info(String.format("Player %S connected on game %S on port %S", player.id, id, port));

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
			if (player != null && player.session == session) {
				seats[i] = null;
				LOGGER.info(String.format("Player %S disconnected on game %S on port %S", player.id, id, port));
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
		LOGGER.info("Destroying room " + id + " on " + port);

		players = new HashMap<Session, Player>();
		List<Player> players = getPlayers();
		for (Player player : players) {
			Session session = player.session;
			if (session.isOpen()) {
				try {
					JSONObject json = PayloadMapper.toJSON(PayloadFactory.DESTROYED());
					session.getBasicRemote().sendText(json.toString(2));
				} catch (JSONException | IOException e) {
					LOGGER.error(e);
				}
			}
		}		
		
		try {
			server.stop();
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			server.destroy();
		}

	}
	
}
