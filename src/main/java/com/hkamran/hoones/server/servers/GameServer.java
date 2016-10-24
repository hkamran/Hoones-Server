package com.hkamran.hoones.server.servers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;

import com.hkamran.hoones.server.Payload;
import com.hkamran.hoones.server.payloads.Controller;
import com.hkamran.hoones.server.payloads.Player;
import com.hkamran.hoones.server.payloads.State;


@ClientEndpoint
@ServerEndpoint(value = "/")
public class GameServer {

	private final static Logger log = LogManager.getLogger(GameServer.class);
	
	public static Map<Session, Player> players = new HashMap<Session, Player>();
	public static Player host;
	public static int counter = 0;

	Status status = Status.PLAYING;
	
	public static enum Status {
		SYNC, WAITING, PLAYING
	}
	
	@OnOpen
	public void OnWebSocketConnect(Session session) {
		counter++;
		Player player = new Player(session, counter);
		players.put(session, player);
		log.info(String.format("Player %S connected on session %S", counter, session.getId()));
		session.setMaxIdleTimeout(600000);
		
		if (players.size() == 1) {
			host = player;

		}
		
		Payload payload = new Payload();
		payload.type = Payload.Type.PLAYER;
		payload.payload = player;
		send(session, payload);		
		
		if (players.size() > 1) {
			synchonize();
		}
	}
	
	@OnClose
	public void onWebSocketClose(Session session) {
		counter--;
		log.info(String.format("Player %d disconnected on session %s ", counter, session.getId()));
		
		players.remove(session);
	}	
	
	@OnMessage
	public void onWebSocketText(String message, Session session) {
		if (status == Status.SYNC) {
			if (host != null && host.session == session) {
				Payload payload = Payload.parseJSON(message);
				if (payload.payload instanceof State) {
					State state = (State) payload.payload;
					if (state.status == State.Status.GET) {
						state.status = State.Status.UPDATE;
						broadcast(payload);
						status = Status.WAITING;
					}
				}
			} 
			return;
		}
		
		if (status == Status.WAITING) {
			//Update player status
			Payload payload = Payload.parseJSON(message);
			if (payload.payload instanceof State) {
				State state = (State) payload.payload;
				if (state.status == State.Status.WAITING) {
					Player player = players.get(session);
					player.status = Player.Status.READY;
				}
			}
			
			//Check if we are all READY!!
			for (Session sess : players.keySet()) {
				Player player = players.get(sess);
				if (player.status == Player.Status.WAITING) {
					return;
				}
			}
			
			//Play
			status = Status.PLAYING;
			broadcast(Payload.PLAY());
			return;
		}
		
		if (status == Status.PLAYING) {
			Payload payload = Payload.parseJSON(message);
			Player player = players.get(session);
			
			if (payload.payload instanceof Controller) {
				Controller controller = (Controller) payload.payload;
				
				Payload resPayload = new Payload();
				resPayload.type = Payload.Type.CONTROLLER;
				resPayload.payload = controller;
				
				Set<Session> ignoring = new HashSet<Session>();
				ignoring.add(session);
				
				broadcast(payload, ignoring);
			} else if (payload.payload instanceof State) {
				State state = (State) payload.payload;
				if (state.status == State.Status.STATUS) {
					player.cycle = state.cycle;
					player.count++;
					
					Boolean isDesynced = isDesynched();
					if (isDesynced) {
						synchonize();
						return;
					}
				}
				
				return;
			}
		}
	
	}	
	
	private Boolean isDesynched() {

		int MAX_UPDATE_DIFF = 3;
		int MAX_CYCLE_DIFF = 5000;
		
		for (Session session : players.keySet()) {
			if (host.session == session) {
				continue;
			}
			Player player = players.get(session);
			
			int updateDiff = Math.abs(host.count - player.count);
			if (updateDiff > MAX_UPDATE_DIFF) {
				return true;
			} else {
				if (updateDiff == 0) {
					int cycleDiff = Math.abs(host.cycle - player.cycle);
					if (cycleDiff > MAX_CYCLE_DIFF) {
						return true;
					}
				}
			}
			
			
		}
		
		return false;
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}	
	
	public void synchonize() {
		log.info("Synchronizing players...");
		
		broadcast(Payload.STOP());
		status = Status.SYNC;
		
		for (Session session : players.keySet()) {
			Player player = players.get(session);
			player.status = Player.Status.WAITING;
		}
		
		State state = new State();
		state.status = State.Status.GET;
		
		Payload payload = new Payload();
		payload.type = Payload.Type.SYNCHRONIZE;
		payload.payload = state;
		
		send(host.session, payload);
	}
	
	public static void send(Session session, Payload payload) {
		try {
			session.getBasicRemote().sendText(payload.toJSON().toString(2));
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void broadcast(Payload payload) {
		Set<Session> ignoring = new HashSet<Session>();
		broadcast(payload, ignoring);
	}
	
	public void broadcast(Payload payload, Set<Session> ignoring) {
		for (Session session: players.keySet()) {	
			if (ignoring.contains(session)) {
				continue;
			}
			
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(payload.toJSON().toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public Player getPlayer(Session session) {
		return players.get(session);
	}

}
