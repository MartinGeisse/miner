/**
 * Copyright (c) 2010 Martin Geisse
 *
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.server.api.account;

import name.martingeisse.api.request.ApiRequestCycle;
import name.martingeisse.common.javascript.analyze.JsonAnalyzer;
import name.martingeisse.common.javascript.jsonbuilder.JsonObjectBuilder;
import name.martingeisse.common.javascript.jsonbuilder.JsonValueBuilder;
import name.martingeisse.webide.entity.Player;
import name.martingeisse.webide.entity.UserAccount;

/**
 * This handler creates a player access token for one of the logged-in
 * user's player characters.
 */
public final class AccessPlayerHandler extends AbstractLoggedInHandler {

	/* (non-Javadoc)
	 * @see name.martingeisse.miner.server.api.account.AbstractLoggedInHandler#handle(name.martingeisse.api.request.RequestCycle, name.martingeisse.common.javascript.analyze.JsonAnalyzer, name.martingeisse.common.javascript.jsonbuilder.JsonValueBuilder, name.martingeisse.webide.entity.UserAccount)
	 */
	@Override
	protected void handle(ApiRequestCycle requestCycle, JsonAnalyzer input, JsonValueBuilder<?> output, UserAccount userAccount) throws Exception {
		Player player = AccountApiUtil.fetchPlayer(input, userAccount);
		String token = AccountApiUtil.createPlayerAccessToken(player.getId());
		JsonObjectBuilder<?> objectBuilder = output.object();
		objectBuilder.property("playerAccessToken").string(token);
		objectBuilder.end();
	}
	
}