/**
 * Copyright (C) 2018 Matthew A Chapman
 * 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package oss.chapman.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author chapmma6
 *
 */
public class ProxyConnection implements EventHandler {
	private Logger log;
	private SimpleProxyStatistics stats;
	private SocketChannel clientChannel; // toward client
	private SocketChannel serverChannel; // toward server
	private SelectionKey serverKey;
	private SelectionKey clientKey;
	private Flow toServer;
	private Flow toClient;
	ConnectionState state;
	long expireTime;
	long startTime;
	public static long IDLE_TIMEOUT_MS=60000;
	
	public ProxyConnection(Selector selector, SocketChannel clientChannel, SimpleProxyConfig config, SimpleProxyStatistics stats, int client_id)
	{
		state = ConnectionState.CONNECTING_TO_SERVER;
		startTime = System.currentTimeMillis();
		expireTime = startTime + IDLE_TIMEOUT_MS; 
		log = Logger.getLogger("ProxyConnection("+client_id+")");
		this.stats = stats;
		this.clientChannel = clientChannel;
		stats.connectionAdded(this);
		try {
			this.clientChannel.configureBlocking(false);

			clientKey = this.clientChannel.register(selector, 0, this);
			log.finest("Registered client SelectionKey "+clientKey);
		} catch (IOException ex) {
			try {
				clientChannel.close();
			} catch (IOException ignored) {}
			stats.connectionRemoved(this);
		}
		initServerConnection(selector, config.targetHost, config.targetPort);		

	}
			
	public void testExpire(long current_time_ms) {
		if (current_time_ms > expireTime) {
			log.info("connection has expired, closing connections.");
			close();
		} else {
			log.finest(""+((expireTime - current_time_ms)/1000L)+" seconds until we expire");
		}
	}
	
	
	public String toString() 
	{
		try {
			return "ConnFrom-"+clientChannel.getRemoteAddress().toString();
		} catch (IOException ex) {
			return "ProxyConnection-Broken";
		}
	}

	public void close() {
		log.info("Closing Connections");
		state=ConnectionState.CLOSED;
		closeServer();
		closeClient();
	}
	
	
	private void closeClient() {
		clientKey.cancel();
		try {
			clientChannel.close();
		} catch (IOException ignored) {
		}		
		stats.connectionRemoved(this);
	}
	
	private void closeServer() {
		if (serverKey != null) {
			serverKey.cancel();
		}
		if (serverChannel != null) {
		
			try {
				serverChannel.close();
			} catch (IOException ignored) {
			}
		}
	}

	private void initServerConnection(Selector selector, String hostname, int port) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Starting DNS lookup of "+hostname);
		}
		// this is a blocking call...
		InetSocketAddress addr = new InetSocketAddress(hostname, port);
		if (addr.isUnresolved()) {
			log.warning("Could not resolve servername "+hostname+" to an ip address, closing connection");
			state = ConnectionState.CLOSED;
			closeClient();
			return;
		}		
		log.fine("resolved upstreamHost "+hostname+" to "+addr.getAddress().toString());

		try {
			serverChannel = SocketChannel.open();
			serverChannel.configureBlocking(false);
			serverKey = serverChannel.register(selector, 0, this);

			if (serverChannel.connect(addr)) {
				log.finer("connect worked right away");
				finishConnect();
			} else {
				log.finer("connect will take time");
				serverKey.interestOps(SelectionKey.OP_CONNECT);
			}

		} catch (IOException ex) {
			log.warning("IOException: could not connect to server, Closing client connection" + ex.getMessage());
			state = ConnectionState.CLOSED;
			close();
		}
	}

	private void finishConnect() {
		try {
			if (serverChannel.finishConnect()) {
				log.info("New Connection Established: "+clientChannel+" to "+serverChannel);
				toServer = new Flow("ToServer", clientKey, serverKey, null);
				toClient = new Flow("ToClient", serverKey, clientKey, toServer);

				serverKey.interestOps(SelectionKey.OP_READ);
				clientKey.interestOps(SelectionKey.OP_READ);
				state = ConnectionState.WAITING_FOR_CLIENT;				
			}
		} catch (IOException ex) {
			this.close();
		}
	}
	
	public void handleEvents(SelectionKey k) throws IOException {
		expireTime = System.currentTimeMillis() + IDLE_TIMEOUT_MS;
		
		
		if (state == ConnectionState.CONNECTING_TO_SERVER) {
			if (k.isConnectable() && k.channel() == serverChannel) {
				finishConnect();
			} else {
				throw new IOException("Connection State is CONNECTING_TO_SERVER and we have unexpected socket events coming in."+k);
			}
		}

		if (k == serverKey) {
			if (k.isWritable()) {
				toServer.handleDataOut();
			}
			if (k.isReadable()) {
				toClient.handleDataIn();
			}			
		} else {
			assert k == clientKey;
			if (k.isWritable()) {
				toClient.handleDataOut();
			}
			if (k.isReadable()) {
				if (state == ConnectionState.WAITING_FOR_CLIENT) {
					state = ConnectionState.RUNNING;
				}
				toServer.handleDataIn();
			}
		}

		if(toServer.outClosed && toClient.outClosed) {
			log.fine("Both directions write is closed, closing connection");
			close();
			return;
		}
		
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Server interest Ops are "+EventLoop.eventType(serverKey.interestOps()));
			log.finest("Client interest Ops are "+EventLoop.eventType(clientKey.interestOps()));
		}	
	}

	public long getStartTime() {
		return startTime;
	}
	
	public long getBytesToServer() {
		return toServer.outBytes;
	}
	public long getBytesToClient() {
		return toClient.outBytes;
	}
}
