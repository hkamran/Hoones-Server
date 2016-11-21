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
import com.hkamran.hoones.server.payloads.Keys;
import com.hkamran.hoones.server.payloads.Player;
import com.hkamran.hoones.server.payloads.State;


@ClientEndpoint
@ServerEndpoint(value = "/")
public class GameServer {

	private final static Logger log = LogManager.getLogger(GameServer.class);

	public static final int SIZE = 2;

	public static Player host;
	public static Session[] room = new Session[SIZE];
	public static Map<Session, Player> players = new HashMap<Session, Player>();
	
	public static Status status = Status.PLAYING;
	
	public static enum Status {
		SYNCING, WAITING, PLAYING
	}
	
	@OnOpen
	public void OnWebSocketConnect(Session session) {
		
		session.setMaxIdleTimeout(600000);
		
		for (int i = 0; i < room.length; i++) {
			if (room[i] != null) continue;
			
			room[i] = session;
			Player player = new Player(session, i + 1);
			players.put(session, player);
			log.info(String.format("Player %S connected on session %S", i + 1, session.getId()));
	
	
			Payload payload = new Payload();
			payload.type = Payload.Type.PLAYER;
			payload.data = player;
			send(session, payload);		
			break;
			
		}
		
		host = null;
		for (int i = 0; i < room.length; i++) {
			if (room[i] == null)  continue;
			if (room[i] != null) {
				host = players.get(room[i]);
				break;
			}
		}
		
		if (players.size() > 1) {
			synchonize();
		}
	}
	
	@OnClose
	public void onWebSocketClose(Session session) {
		
		for (int i = 0; i < room.length; i++) {
			if (session == room[i]) {
				log.info(String.format("Player %d disconnected on session %s ", i + 1, session.getId()));
				room[i] = null;
				break;
			}
		}
		players.remove(session);
	}	
	
	@OnMessage
	public void onWebSocketText(String message, Session session) {
		if (status == Status.SYNCING) {
			if (host != null && host.session == session) {
				Payload payload = Payload.parseJSON(message);
				if (payload.type == Payload.Type.GET) {
					log.info("Broadcasting state of the host");
					payload.type = Payload.Type.PUT;
					broadcast(payload);
					status = Status.WAITING;
				}
			} 
			return;
		}
		
		if (status == Status.WAITING) {
			//Update player status
			Payload payload = Payload.parseJSON(message);
			if (payload.type == Payload.Type.WAITING) {
				Player player = players.get(session);
				player.status = Player.Status.READY;
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
			
			if (payload.type == Payload.Type.KEYS) {
				Keys controller = (Keys) payload.data;
				log.info("Received Keys from " + controller.playerId + " at " + controller.cycle);
				
				Payload resPayload = new Payload();
				resPayload.type = Payload.Type.KEYS;
				resPayload.data = controller;
				

				broadcast(payload);
			} else if (payload.type == Payload.Type.PUT) {
				log.info("Resynchronizing clients");
				synchonize();
			}
		 }

	
	}	
	
    static String[] say_what_you_see(String[] inputs) {
        
        String[] solutions = new String[inputs.length];
        
        int k = 0;
        for (String input : inputs) {
            StringBuffer solution = new StringBuffer();
            for (int i = 0; i < input.length(); i++) {
                char letter = input.charAt(i);
                //peek
                int j = i + 1;
                while (j < input.length() && letter == input.charAt(j)) {
                    j++;
                }
                solution.append((j - i) + "" + letter);
                i = j - 1;
            }
            solutions[k] = solution.toString();
            k++;
        }
        
        return solutions;	

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
		status = Status.SYNCING;
		
		for (Session session : players.keySet()) {
			Player player = players.get(session);
			player.status = Player.Status.WAITING;
		}
		
		Payload payload = new Payload();
		payload.type = Payload.Type.GET;
		
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
		
		synchronized(players){
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
	}
	
	
	
	public Player getPlayer(Session session) {
		return players.get(session);
	}
	
	
    static Map<Character, String> patterns = new HashMap<Character, String>();

    static int wordpattern(String pattern, String input) {
        return patternHelper(pattern, 0, input, 0);
    }

    static int patternHelper(String pattern, int i, String input, int j) {
        if (i >= pattern.length() && j >= input.length()) {
            return 1;
        } else {
            if (i >= pattern.length()) return 0;
            Character letter = pattern.charAt(i);
            
            //if we have a mapping then we find one that matches it
            if (patterns.containsKey(letter)) {
                String patternStr = patterns.get(letter);
                StringBuffer subStr = new StringBuffer();
                for (int k = j; k < input.length(); k++) {
                    subStr.append(input.charAt(k));
                    if (subStr.toString().equals(patternStr)) {
                        int result = patternHelper(pattern, i + 1, input, k + 1);
                        if (result == 1) {
                        	return result;
                        }
                    }
                }
            //create a mapping
            } else {
                StringBuffer subStr = new StringBuffer();
                for (int k = j; k < input.length(); k++) {
                    subStr.append(input.charAt(k));
                    patterns.put(letter, subStr.toString());
                    int result = patternHelper(pattern, i + 1, input, k + 1);
                    if (result == 1) {
                        return 1;
                    }
                    patterns.remove(letter);
                }
            }
            
            return 0;
        }
    }
	
	
	public static void main(String[] args) {
		System.out.println("13.37".split("\\.").length);
	}

}
