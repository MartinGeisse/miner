/**
 * Copyright (c) 2010 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.server.tools;

import external.BCrypt;
import name.martingeisse.miner.common.Constants;

/**
 * Helper application to build a new user account.
 */
public class NewUserAccountMain {

	/**
	 * The main method.
	 * @param args command-line arguments (ignored)
	 */
	public static void main(String[] args) {

		// parameters
		String username = "test";
		String password = "test";

		// prepare data
		String salt = BCrypt.gensalt(Constants.BCRYPT_COST);
		System.out.println("salt: " + salt);
		String hash = BCrypt.hashpw(password, salt);
		System.out.println("hash: " + hash);

		// Insert data. Trolololo injection vulnerability, but I'm going to execute the query manually anyway.
		System.out.println();
		System.out.println("INSERT INTO \"miner\".\"UserAccount\" (\"username\", \"passwordHash\") VALUES ('" + username + "', '" + hash + "');");

	}

}
