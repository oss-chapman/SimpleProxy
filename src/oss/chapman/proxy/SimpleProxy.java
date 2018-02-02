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

/**
 * @author chapmma6
 *
 */

public class SimpleProxy {

	EventLoop eventLoop;
	ProxyListener listener;
	
	private SimpleProxy(SimpleProxyConfig config) {
		
		try {
			eventLoop = new EventLoop();
			listener = new ProxyListener(eventLoop.getSelector(), config);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(11);
		}
	}
	
	void run () {
		try {
		
		eventLoop.run();
		} catch (Exception ex ) {
			ex.printStackTrace();
			System.exit(12);
		} finally {
			try {
				listener.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
				
		SimpleProxyConfig config = SimpleProxyConfig.fromArgs(args);
		
		SimpleProxy sp = new SimpleProxy(config);
	
		sp.run();
		
	}

}
