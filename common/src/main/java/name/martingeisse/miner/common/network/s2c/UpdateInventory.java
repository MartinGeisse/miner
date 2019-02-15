package name.martingeisse.miner.common.network.s2c;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import name.martingeisse.miner.common.logic.ItemType;
import name.martingeisse.miner.common.network.BufferUtil;
import name.martingeisse.miner.common.network.Message;
import name.martingeisse.miner.common.network.MessageDecodingException;

/**
 *
 */
public final class UpdateInventory extends Message {

	private final ImmutableList<Element> elements;

	public UpdateInventory(ImmutableList<Element> elements) {
		this.elements = elements;
	}

	public ImmutableList<Element> getElements() {
		return elements;
	}

	@Override
	protected int getExpectedBodySize() {
		return -1;
	}

	@Override
	protected void encodeBody(ByteBuf buffer) {
		BufferUtil.encodeList(elements, Element::encode, buffer);
	}

	public static UpdateInventory decodeBody(ByteBuf buffer) throws MessageDecodingException {
		return new UpdateInventory(BufferUtil.decodeList(Element::decode, buffer));
	}

	public static final class Element {

		private final long id;
		private final ItemType type;
		private final int quantity;
		private final boolean equipped;

		public Element(long id, ItemType type, int quantity, boolean equipped) {
			this.id = id;
			this.type = type;
			this.quantity = quantity;
			this.equipped = equipped;
		}

		public long getId() {
			return id;
		}

		public ItemType getType() {
			return type;
		}

		public int getQuantity() {
			return quantity;
		}

		public boolean isEquipped() {
			return equipped;
		}

		public void encode(ByteBuf buffer) {
			buffer.writeLong(id);
			buffer.writeInt(type.ordinal());
			buffer.writeInt(quantity);
			buffer.writeBoolean(equipped);
		}

		public static Element decode(ByteBuf buffer) {
			return new Element(buffer.readLong(), ItemType.values()[buffer.readInt()], buffer.readInt(), buffer.readBoolean());
		}

	}

}
