/**
 * Copyright (c) 2010 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.client.ingame.player;

/**
 * Represents another player whose data gets updated from the server.
 * Updating replaces the player proxy object, so the fields in this
 * class can be final.
 * <p>
 * TODO the "workingSet" reference in this class is currently always null.
 */
public final class PlayerProxy extends PlayerBase {

	private String name;

	public PlayerProxy() {
		super(null);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
