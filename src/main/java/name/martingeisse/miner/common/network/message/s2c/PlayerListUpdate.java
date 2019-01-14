/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.miner.common.network.message.s2c;

import com.google.common.collect.ImmutableList;
import name.martingeisse.miner.common.geometry.angle.EulerAngles;
import name.martingeisse.miner.common.geometry.vector.Vector3d;
import name.martingeisse.miner.common.network.StackdPacket;
import name.martingeisse.miner.common.network.message.Message;
import name.martingeisse.miner.common.network.message.MessageCodes;
import name.martingeisse.miner.common.network.message.MessageDecodingException;
import org.jboss.netty.buffer.ChannelBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class PlayerListUpdate extends Message {

	private final ImmutableList<Element> elements;

	public PlayerListUpdate(ImmutableList<Element> elements) {
		this.elements = elements;
	}

	public ImmutableList<Element> getElements() {
		return elements;
	}

	@Override
	public StackdPacket encodePacket() {
		StackdPacket packet = new StackdPacket(MessageCodes.S2C_PLAYER_LIST_UPDATE, elements.size() * Element.ENCODED_SIZE);
		for (Element element : elements) {
			element.encode(packet.getBuffer());
		}
		return packet;
	}

	public static PlayerListUpdate decodeBody(ChannelBuffer buffer) throws MessageDecodingException {
		List<Element> elements = new ArrayList<>();
		while (buffer.readableBytes() >= Element.ENCODED_SIZE) {
			elements.add(Element.decode(buffer));
		}
		return new PlayerListUpdate(ImmutableList.copyOf(elements));
	}

	public static final class Element {

		public static final int ENCODED_SIZE = 4 + Vector3d.ENCODED_SIZE + EulerAngles.ENCODED_SIZE;

		private final int id;
		private final Vector3d position;
		private final EulerAngles angles;

		public Element(int id, Vector3d position, EulerAngles angles) {
			this.id = id;
			this.position = position;
			this.angles = angles;
		}

		public int getId() {
			return id;
		}

		public Vector3d getPosition() {
			return position;
		}

		public EulerAngles getAngles() {
			return angles;
		}

		public void encode(ChannelBuffer buffer) {
			buffer.writeInt(id);
			position.encode(buffer);
			angles.encode(buffer);
		}

		public static Element decode(ChannelBuffer buffer) {
			return new Element(buffer.readInt(), Vector3d.decode(buffer), EulerAngles.decode(buffer));
		}

	}

}