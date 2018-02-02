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
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;

/**
 * @author chapmma6
 *
 */
public class EventLoop {
	private Selector selector;
	private Logger log;
	
	public EventLoop() throws java.io.IOException 
	{
		log = Logger.getLogger("EventLoop");
		selector = Selector.open();
	}
	
	public static String eventType(int evt) {
		String ret = "";
		int tmp = evt;
		
		if ((tmp & SelectionKey.OP_ACCEPT) != 0) {
			ret = ret + "A";
			tmp &= ~SelectionKey.OP_ACCEPT;
		}
		if ((tmp & SelectionKey.OP_CONNECT) != 0) {
			ret = ret + "C";
			tmp &= ~SelectionKey.OP_CONNECT;
		}
		if ((tmp & SelectionKey.OP_READ) != 0) {
			ret = ret + "R";
			tmp &= ~SelectionKey.OP_READ;
		}
		if ((tmp & SelectionKey.OP_WRITE) != 0) {
			ret = ret + "W";
			tmp &= ~SelectionKey.OP_WRITE;
		}

		if (tmp != 0) {
			ret = ret + "(unk-"+tmp+")";
		}
		return ret;
	}
	
	public Selector getSelector() {
		return selector;
	}
	
	public void run() throws java.io.IOException
	{
		
		int loops = -1;
		while (loops != 0) {
			if (loops > 0) {
				loops--;
			}
			log.finest("enter select()");
			int ret = selector.select(10000L);
			log.finest(""+ret+" of "+selector.keys().size()+" keys are ready");
			for(SelectionKey k : selector.selectedKeys()) {
				log.finest("Event ["+eventType(k.readyOps())+"] from "+k.channel());
				((EventHandler) k.attachment()).handleEvents(k);
			}
			selector.selectedKeys().clear();
			long current_time_ms = System.currentTimeMillis();
			for (SelectionKey k : selector.keys()) {
				((EventHandler) k.attachment()).testExpire(current_time_ms);
			}
			
		}	
	}	
}
