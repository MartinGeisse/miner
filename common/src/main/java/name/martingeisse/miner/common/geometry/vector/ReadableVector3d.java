/**
 * Copyright (c) 2014 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.common.geometry.vector;

import io.netty.buffer.ByteBuf;

/**
 *
 */
public abstract class ReadableVector3d {

	public abstract double getX();

	public abstract double getY();

	public abstract double getZ();

	public abstract Vector3d freeze();

	public final void encode(ByteBuf buffer) {
		buffer.writeDouble(getX());
		buffer.writeDouble(getY());
		buffer.writeDouble(getZ());
	}

	public static final int ENCODED_SIZE = 24;

}
