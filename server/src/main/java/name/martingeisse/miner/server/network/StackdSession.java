/**
 * Copyright (c) 2010 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.server.network;

import com.google.common.collect.ImmutableList;
import name.martingeisse.miner.common.cubetype.CubeType;
import name.martingeisse.miner.common.cubetype.CubeTypes;
import name.martingeisse.miner.common.geometry.vector.Vector3i;
import name.martingeisse.miner.common.logic.EquipmentSlot;
import name.martingeisse.miner.common.network.Message;
import name.martingeisse.miner.common.network.c2s.*;
import name.martingeisse.miner.common.network.c2s.request.CreatePlayerRequest;
import name.martingeisse.miner.common.network.c2s.request.DeletePlayerRequest;
import name.martingeisse.miner.common.network.c2s.request.LoginRequest;
import name.martingeisse.miner.common.network.c2s.request.Request;
import name.martingeisse.miner.common.network.s2c.*;
import name.martingeisse.miner.common.network.s2c.response.ErrorResponse;
import name.martingeisse.miner.common.network.s2c.response.Response;
import name.martingeisse.miner.common.section.SectionDataId;
import name.martingeisse.miner.common.section.SectionDataType;
import name.martingeisse.miner.common.section.SectionId;
import name.martingeisse.miner.common.util.UserVisibleMessageException;
import name.martingeisse.miner.server.game.Player;
import name.martingeisse.miner.server.game.PlayerListener;
import name.martingeisse.miner.server.game.UserAccount;
import name.martingeisse.miner.server.game.UserAccountRepository;
import name.martingeisse.miner.server.postgres_entities.PlayerInventorySlotRow;
import name.martingeisse.miner.server.world.WorldSubsystem;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
	private volatile UserAccount userAccount; // TODO authorize e.g. player acces against this user account!
	private volatile Player player;
	private volatile Avatar avatar;

	public StackdSession(StackdServer server, ServerEndpoint endpoint) {
		this.server = server;
		this.endpoint = endpoint;
	}

	//
	// user account management
	//

	//
	// player management
	//

	public void selectPlayer(long playerId) {
		if (player != null) {
			throw new IllegalStateException("player already selected");
		}
		if (avatar != null) {
			throw new IllegalStateException("cannot select player -- avatar exists (state inconsistent)");
		}
		player = new Player(playerId);
		player.add(new PlayerListener() {

			@Override
			public void onCoinsChanged() {
				sendCoinsUpdate();
			}

			@Override
			public void onFlashMessage(String message) {
				sendFlashMessage(message);
			}

			@Override
			public void onInventoryChanged() {
				sendInventoryUpdate();
			}

		});
		sendCoinsUpdate();
		sendInventoryUpdate();
	}

	public Player getPlayer() {
		return player;
	}

	//
	// avatar management
	//

	public void createAvatar() {
		if (player == null) {
			throw new IllegalStateException("cannot create avatar -- no player selected");
		}
		if (avatar != null) {
			throw new IllegalStateException("avatar already exists");
		}
		avatar = new Avatar();
		player.loadAvatar(avatar);
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
		if (player != null && avatar != null) {
			player.saveAvatar(avatar);
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
		send(new UpdateCoins(player == null ? 0 : player.getCoins()));
	}

	public void sendInventoryUpdate() {
		if (player == null) {
			return;
		}
		List<PlayerInventorySlotRow> slots = player.getInventory().listAll();
		List<UpdateInventory.Element> updateElements = new ArrayList<>();
		for (PlayerInventorySlotRow slot : slots) {
			CubeType type = CubeTypes.CUBE_TYPES[slot.getType()];
			updateElements.add(new UpdateInventory.Element(slot.getId(), type, slot.getQuantity(), slot.getEquipped()));
		}
		send(new UpdateInventory(ImmutableList.copyOf(updateElements)));
	}

	@Override
	public void consumeInteractiveSectionDataResponse(InteractiveSectionDataResponse response) {
		send(response);
	}

	final void onMessageReceived(Message untypedMessage) {
		if (untypedMessage instanceof Request) {

			Request message = (Request) untypedMessage;
			try {
				Response response = onRequest(message);
				if (response == null) {
					logger.error("onRequest() returned null");
					send(new ErrorResponse("internal server error"));
				} else {
					send(response);
				}
			} catch (UserVisibleMessageException e) {
				send(new ErrorResponse(e.getMessage()));
			} catch (Exception e) {
				logger.error("exception during request handling", e);
				send(new ErrorResponse("internal server error"));
			}

		} else if (untypedMessage instanceof ResumePlayer) {

			if (userAccount != null) {
				ResumePlayer message = (ResumePlayer) untypedMessage;
				selectPlayer(message.getId());
				createAvatar();
			}

		} else if (untypedMessage instanceof UpdatePosition) {

			if (avatar != null) {
				UpdatePosition message = (UpdatePosition) untypedMessage;
				avatar.setPosition(message.getPosition());
				avatar.setOrientation(message.getOrientation());
			}
		} else if (untypedMessage instanceof PlaceCube) {

			PlaceCube message = (PlaceCube) untypedMessage;
			Vector3i position = message.getPosition();
			PlayerInventorySlotRow slot = player.getInventory().getEquippedByEquipmentSlot(EquipmentSlot.HAND);
			if (slot != null) {

				// handle stairs direction
				int cubeTypeIndex = slot.getType();
				if (cubeTypeIndex == 50) {
					switch (message.getDirection()) {
						case POSITIVE_X:
							cubeTypeIndex = 52;
							break;
						case NEGATIVE_X:
							cubeTypeIndex = 53;
							break;
						case POSITIVE_Z:
							cubeTypeIndex = 51;
							break;
						case NEGATIVE_Z:
							cubeTypeIndex = 50;
							break;
					}
				}

				CubeType cubeType = CubeTypes.CUBE_TYPES[cubeTypeIndex];
				server.getWorldSubsystem().placeCube(position, cubeType);
			}

		} else if (untypedMessage instanceof InteractiveSectionDataRequest) {

			InteractiveSectionDataRequest message = (InteractiveSectionDataRequest) untypedMessage;
			SectionId sectionId = message.getSectionId();
			SectionDataType type = SectionDataType.INTERACTIVE;
			final SectionDataId dataId = new SectionDataId(sectionId, type);
			server.getWorldSubsystem().addJob(dataId, this);

		} else if (untypedMessage instanceof DigNotification) {

			DigNotification message = (DigNotification) untypedMessage;
			server.getWorldSubsystem().dig(player, message.getPosition());

		} else if (untypedMessage instanceof EquipMessage || untypedMessage instanceof ApplyFormula) {

			player.handleMessage(untypedMessage);

		} else {
			logger.error("unknown message: " + untypedMessage);
		}
	}

	private Response onRequest(Request request) {
		if (request instanceof LoginRequest) {

			LoginRequest message = (LoginRequest) request;
			UserAccount userAccount = UserAccountRepository.INSTANCE.login(message.getUsername(), message.getPassword());
			this.userAccount = userAccount;
			return userAccount.getLoginResponse();

		} else if (request instanceof CreatePlayerRequest) {

			if (userAccount == null) {
				throw new UserVisibleMessageException("not logged in");
			}
			CreatePlayerRequest message = (CreatePlayerRequest) request;
			return userAccount.createPlayer(message);

		} else if (request instanceof DeletePlayerRequest) {

			if (userAccount == null) {
				throw new UserVisibleMessageException("not logged in");
			}
			DeletePlayerRequest message = (DeletePlayerRequest) request;
			return userAccount.deletePlayer(message);

		} else {
			throw new IllegalArgumentException("unknown request message: " + request);
		}
	}

}
