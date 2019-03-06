package com.hkamran.hoones.server.dos;

public class Keys {
	
	public int[] data = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
	public int playerId;
	public int cycle;
	
	public Keys(Integer id, Integer cycle) {
		this.playerId = id;
		this.cycle = cycle;
	}
	
	public Keys(Integer id, Integer cycle, int[] keys) {
		this.playerId = id;
		this.cycle = cycle;
		this.data = keys;
	}

}
