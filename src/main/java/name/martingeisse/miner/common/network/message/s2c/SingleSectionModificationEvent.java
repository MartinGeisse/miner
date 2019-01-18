package name.martingeisse.miner.common.network.message.s2c;

import name.martingeisse.miner.common.geometry.SectionId;
import name.martingeisse.miner.common.network.StackdPacket;
import name.martingeisse.miner.common.network.message.Message;
import name.martingeisse.miner.common.network.message.MessageCodes;
import name.martingeisse.miner.common.network.message.MessageDecodingException;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Sent by the server when a section gets modified. Clients that are close enough to be interested in the update would
 * typically request the new section render model and collider in turn. Clients that are too far away would ignore
 * these events. TODO: store the client's rough position on the server, filter mod events server-side, then just send
 * the updated objects. This slightly increases network traffic (sending additional data) but reduces latency and
 * simplifies the code.
 */
public final class SingleSectionModificationEvent extends Message {

	private final SectionId sectionId;

	public SingleSectionModificationEvent(SectionId sectionId) {
		this.sectionId = sectionId;
	}

	public SectionId getSectionId() {
		return sectionId;
	}

	@Override
	public StackdPacket encodePacket() {
		StackdPacket packet = new StackdPacket(MessageCodes.S2C_SINGLE_SECTION_MODIFICATION_EVENT, SectionId.ENCODED_SIZE);
		ChannelBuffer buffer = packet.getBuffer();
		sectionId.encode(buffer);
		return packet;
	}

	public static SingleSectionModificationEvent decodeBody(ChannelBuffer buffer) throws MessageDecodingException {
		return new SingleSectionModificationEvent(SectionId.decode(buffer));
	}

}