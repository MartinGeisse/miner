/**
 * Copyright (c) 2014 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.common.geometry.vector;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 *
 */
public abstract class ReadableVector2i {

	public abstract int getX();
	public abstract int getY();

	public abstract Vector2i freeze();

	public final void encode(ChannelBuffer buffer) {
		buffer.writeInt(getX());
		buffer.writeInt(getY());
	}

	public static final int ENCODED_SIZE = 8;

}
