/**
 * Copyright (c) 2011 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.common.util;

/**
 * Utility methods to handle generic types.
 */
public class GenericTypeUtil {

	/**
	 * Prevent instantiation.
	 */
	private GenericTypeUtil() {
	}

	/**
	 * Converts the argument reference to type T without a runtime type check.
	 * @param <T> the target type
	 * @param o the reference to convert
	 * @return the converted reference
	 */
	@SuppressWarnings("unchecked")
	public static <T> T unsafeCast(Object o) {
		return (T) o;
	}

}
