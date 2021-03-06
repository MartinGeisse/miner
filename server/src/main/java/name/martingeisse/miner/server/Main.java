/**
 * Copyright (c) 2010 Martin Geisse
 * <p>
 * This file is distributed under the terms of the MIT license.
 */

package name.martingeisse.miner.server;

import com.datastax.driver.core.Cluster;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import name.martingeisse.miner.common.Constants;
import name.martingeisse.miner.common.task.TaskSystem;
import name.martingeisse.miner.server.network.ServerChannelInitializer;
import name.martingeisse.miner.server.network.StackdServer;
import name.martingeisse.miner.server.util.database.postgres.PostgresService;

import java.io.File;
import java.net.InetSocketAddress;

/**
 * The main class for the game server.
 */
public class Main {

	/**
	 * The main method.
	 *
	 * @param args command-line arguments (ignored)
	 * @throws Exception on errors
	 */
	public static void main(final String[] args) throws Exception {

		// parse the command line
		if (args.length == 0) {
			Configuration.initializeFromClasspathConfig();
		} else if (args.length == 1) {
			Configuration.initializeFromConfigFile(new File(args[0]));
		} else {
			System.err.println("usage: Main [config.properties]");
			System.exit(1);
		}

		// initialize task system
		TaskSystem.initialize();

		// initialize SQL database
		Databases.main = new PostgresService();
		Databases.main.setPostgresHost("localhost");
		Databases.main.setPostgresDatabaseName("miner");
		Databases.main.setPostgresUser("postgres");
		Databases.main.setPostgresPassword("postgres");

		// initialize Cassandra database
		Databases.cassandraCluster = Cluster.builder().addContactPoint("localhost").build();
		Databases.world = Databases.cassandraCluster.connect("miner");

		// initialize network server
		StackdServer server = new StackdServer();
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		final ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.option(ChannelOption.SO_BACKLOG, 128);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.childHandler(new ServerChannelInitializer(server));
		ChannelFuture bindFuture = bootstrap.bind(new InetSocketAddress(Constants.NETWORK_PORT));
		try {
			bindFuture.sync();
		} catch (InterruptedException e) {
			throw new RuntimeException("failed to wait for binding the socket");
		}

	}

}
