/**
 * Copyright (c) 2010 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.server.network;

import name.martingeisse.common.SecurityTokenUtil;
import name.martingeisse.miner.common.network.Message;
import name.martingeisse.miner.common.network.c2s.*;
import name.martingeisse.miner.common.network.s2c.FlashMessage;
import name.martingeisse.miner.common.network.s2c.InteractiveSectionDataResponse;
import name.martingeisse.miner.common.network.s2c.PlayerResumed;
import name.martingeisse.miner.common.network.s2c.UpdateCoins;
import name.martingeisse.miner.common.section.SectionDataId;
import name.martingeisse.miner.common.section.SectionDataType;
import name.martingeisse.miner.common.section.SectionId;
import name.martingeisse.miner.server.MinerServerSecurityConstants;
import name.martingeisse.miner.server.game.DigUtil;
import name.martingeisse.miner.server.game.PlayerAccess;
import name.martingeisse.miner.server.world.WorldSubsystem;
import org.apache.log4j.Logger;
import org.joda.time.Instant;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Stores the data for one user session (currently associated with the connection,
 * but intended to service connection dropping and re-connecting).
 * <p>
 * The session stores per-client, per-subsystem state: user account, player, avatar, and so on. It routes network
 * messages to these objects. It also consumes a few network messages itself that affect creation / deletion of
 * subsystem state, e.g. logging in, loading a player and creating an avatar. Subsystems should shield themselves
 * against knowing about the session by using role interfaces.
 * <p>
 * Threading model: All message handling coming from the session is done in Netty threads and should therefore finish
 * quickly. Use the task system for long-lasting stuff.
 */
public class StackdSession implements WorldSubsystem.SectionDataConsumer {

	private static Logger logger = Logger.getLogger(StackdSession.class);

	private final StackdServer server;
	private final ServerEndpoint endpoint;
	private volatile PlayerAccess playerAccess;
	private volatile Avatar avatar;

	public StackdSession(StackdServer server, ServerEndpoint endpoint) {
		this.server = server;
		this.endpoint = endpoint;
	}

	//
	// player management
	//

	public void selectPlayer(long playerId) {
		if (playerAccess != null) {
			throw new IllegalStateException("player already selected");
		}
		if (avatar != null) {
			throw new IllegalStateException("cannot select player -- avatar exists (state inconsistent)");
		}
		playerAccess = new PlayerAccess(playerId);
		playerAccess.add(new PlayerAccess.Listener() {

			@Override
			public void onCoinsChanged() {
				sendCoinsUpdate();
			}

			@Override
			public void onFlashMessage(String message) {
				sendFlashMessage(message);
			}

		});
		sendCoinsUpdate();
	}

	public PlayerAccess getPlayerAccess() {
		return playerAccess;
	}

	//
	// avatar management
	//

	public void createAvatar() {
		if (playerAccess == null) {
			throw new IllegalStateException("cannot create avatar -- no player selected");
		}
		if (avatar != null) {
			throw new IllegalStateException("avatar already exists");
		}
		avatar = new Avatar();
		playerAccess.loadAvatar(avatar);
		send(new PlayerResumed(avatar.getPosition(), avatar.getOrientation()));
	}

	public Avatar getAvatar() {
		return avatar;
	}

	//
	// networking
	//

	/**
	 * Called after this session has been created and registered with the server.
	 */
	public void onConnect() {
		sendFlashMessage("Connected to server.");
	}

	/**
	 * Called when the client disconnects, just before the session gets removed from the server.
	 */
	public void onDisconnect() {
		if (playerAccess != null && avatar != null) {
			playerAccess.saveAvatar(avatar);
		}
	}

	public void send(Message message) {
		endpoint.send(message);
	}

	/**
	 * Sends a flash message to the client that owns this session.
	 *
	 * @param message the message
	 */
	public void sendFlashMessage(String message) {
		send(new FlashMessage(message));
	}

	/**
	 * Sends an update for the number of coins to the client, fetching the
	 * number of coins from the database.
	 */
	public void sendCoinsUpdate() {
		send(new UpdateCoins(playerAccess == null ? 0 : playerAccess.getCoins()));
	}

	@Override
	public void consumeInteractiveSectionDataResponse(InteractiveSectionDataResponse response) {
		send(response);
	}

	final void onMessageReceived(Message untypedMessage) {
		if (untypedMessage instanceof ResumePlayer) {

			ResumePlayer message = (ResumePlayer) untypedMessage;
			String token = new String(message.getToken(), StandardCharsets.UTF_8);
			String tokenSubject = SecurityTokenUtil.validateToken(token, new Instant(), MinerServerSecurityConstants.SECURITY_TOKEN_SECRET);
			long playerId = Long.parseLong(tokenSubject);
			selectPlayer(playerId);
			createAvatar();

		} else if (untypedMessage instanceof UpdatePosition) {

			if (avatar != null) {
				UpdatePosition message = (UpdatePosition) untypedMessage;
				avatar.setPosition(message.getPosition());
				avatar.setOrientation(message.getOrientation());
			}
		} else if (untypedMessage instanceof CubeModification) {

			server.getWorldSubsystem().handleMessage((CubeModification) untypedMessage);

		} else if (untypedMessage instanceof InteractiveSectionDataRequest) {

			InteractiveSectionDataRequest message = (InteractiveSectionDataRequest) untypedMessage;
			SectionId sectionId = message.getSectionId();
			SectionDataType type = SectionDataType.INTERACTIVE;
			final SectionDataId dataId = new SectionDataId(sectionId, type);
			server.getWorldSubsystem().addJob(dataId, this);

		} else if (untypedMessage instanceof DigNotification) {

			DigNotification message = (DigNotification) untypedMessage;
			WorldSubsystem worldSubsystem = server.getWorldSubsystem();

			// check if successful and remove the cube
			byte oldCubeType = worldSubsystem.getCube(message.getPosition());
			boolean success;
			if (oldCubeType == 1 || oldCubeType == 5 || oldCubeType == 15) {
				success = true;
			} else {
				success = (new Random().nextInt(3) < 1);
			}
			if (!success) {
				// TODO enable god mode -- digging always succeeds
				// break;
			}
			worldSubsystem.setCube(message.getPosition(), (byte) 0);

			// trigger special logic (e.g. add a unit of ore to the player's inventory)
			if (playerAccess != null) {
				DigUtil.onCubeDugAway(playerAccess, message.getPosition(), oldCubeType);
			}

		} else {
			logger.error("unknown message: " + untypedMessage);
		}
	}

}
