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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.util.logging.Logger;

/**
 * @author chapmma6
 *
 */
public class ProxyListener implements EventHandler {
	private Logger log;
	private Selector selector;
	private ServerSocketChannel listenSocket;
	private SimpleProxyStatistics stats;
	String upstreamHost;
	int upstreamPort;
	
	int client_count;
	private SimpleProxyConfig config;

	ProxyListener(Selector selector,
			SimpleProxyConfig config
) throws IOException
	{
		this.config = config;
		this.log = Logger.getLogger("SimpleProxyListener");
		this.stats = new SimpleProxyStatistics(config);
		this.selector = selector;
		this.client_count=0;
		this.upstreamHost = this.config.targetHost;
		this.upstreamPort = this.config.targetPort;
		
		InetSocketAddress listenAddr = new InetSocketAddress(this.config.listenHost, this.config.listenPort);
		
		listenSocket = ServerSocketChannel.open();
		listenSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		listenSocket.bind(listenAddr, 10);
		listenSocket.configureBlocking(false);
		log.info("Listening on "+listenAddr.toString());
		SelectionKey key = listenSocket.register(this.selector, SelectionKey.OP_ACCEPT, this);
		log.finest("Registered SelectionKey "+key);
	}
	
	public void close() throws IOException {
		listenSocket.close();
	}
	
	public void testExpire(long current_time_ms) {
		stats.timerTick(current_time_ms);
	}
	
	/* (non-Javadoc)
	 * @see local.chapman.simpleproxy.EventHandler#handleEvents(java.nio.channels.SelectionKey)
	 */
	public void handleEvents(SelectionKey k) throws IOException {
		// accept up to 5 new connections per select.
		assert (k.isAcceptable());
	
		SocketChannel downstream_conn = listenSocket.accept();
		if (downstream_conn != null) {
			log.fine("Accepting connection from "+downstream_conn.toString() );
			new ProxyConnection(this.selector, downstream_conn, config, stats, ++this.client_count);
		}
	}
	
	void register(ProxyConnection conn) {

	}
	
	void remove(ProxyConnection conn) {
	
	}
}
