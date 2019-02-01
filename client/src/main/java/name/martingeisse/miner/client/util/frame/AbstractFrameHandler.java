/**
 * Copyright (c) 2010 Martin Geisse
 *
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.client.util.frame;

import name.martingeisse.miner.client.util.glworker.GlWorkerLoop;

/**
 * Base implementation of {@link IFrameHandler}.
 */
public abstract class AbstractFrameHandler implements IFrameHandler {

	/* (non-Javadoc)
	 * @see name.martingeisse.stackd.frame.IFrameHandler#handleStep()
	 */
	@Override
	public void handleStep() throws BreakFrameLoopException {
	}

	/* (non-Javadoc)
	 * @see name.martingeisse.stackd.client.frame.IFrameHandler#onBeforeDraw(name.martingeisse.glworker.GlWorkerLoop)
	 */
	@Override
	public void onBeforeDraw(GlWorkerLoop glWorkerLoop) {
	}
	
	/* (non-Javadoc)
	 * @see name.martingeisse.stackd.client.frame.IFrameHandler#draw(name.martingeisse.glworker.GlWorkerLoop)
	 */
	@Override
	public void draw(GlWorkerLoop glWorkerLoop) {
	}

}
