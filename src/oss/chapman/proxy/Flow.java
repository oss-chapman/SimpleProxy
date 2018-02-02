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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * @author chapmma6
 *
 */
public class Flow {
	Logger log;
	ByteBuffer buf;
	SelectionKey inKey;
	SelectionKey outKey;
	
	boolean inClosed;
	boolean outClosed;
	Flow sibling;

	long outBytes = 0L;

	
	Flow(String name, SelectionKey inKey, SelectionKey outKey, Flow sibling)
	{
		log = Logger.getLogger(name);
		this.buf = ByteBuffer.allocate(1024);
		this.inKey = inKey;
		this.outKey = outKey;
		this.sibling = sibling;
		inClosed = false;
		outClosed = false;
		
		
		this.inKey.interestOps(SelectionKey.OP_READ);
	}
	
		
	
	void handleDataIn() {
		if (inClosed == false) {
			SocketChannel inChannel = (SocketChannel)inKey.channel();
			try {
				long ret = inChannel.read(buf);
				log.finest("read "+ret+" bytes");
				if (ret == -1) {
					log.info("read side closed");
					inClosed = true;
					if (buf.position() == 0) {
						log.info("buffer is flushed, close write side too");
						outClosed = true;
						if (sibling != null) {
							sibling.flushAndClose();
						}
					}
				}
				
			} catch (IOException ex) {
				log.info("read IOException, closing read");
				inClosed = true;
			}
		}

		if (inClosed || (buf.hasRemaining() == false)) {
			inKey.interestOps(inKey.interestOps() & ~ SelectionKey.OP_READ);
		} else {
			inKey.interestOps(inKey.interestOps() | SelectionKey.OP_READ);
		}
		if (buf.position() > 0) {
			outKey.interestOps(outKey.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	void handleDataOut() {
		if ( outClosed ) {
			if (buf.position() > 0) {
				log.warning("dropped "+buf.position()+" bytes due to close");
				buf.clear();
			}
		} else {
			if ( buf.position() > 0 ) {
				SocketChannel outChannel = (SocketChannel)outKey.channel();
				buf.flip();
				try {
					long ret = outChannel.write(buf);
					log.finest("wrote "+ret+" bytes");
					outBytes += ret;
				} catch (IOException ex) {
					log.info("Write IOException, closing flow");
					outClosed = true;
				}
				buf.compact();
			}
			if (inClosed && buf.position() == 0) {
				log.fine("input is closed, and we just flushed, close output");
				outClosed = true;
				if (sibling != null) {
					sibling.flushAndClose();
				}
			}
		}
		
		if (outClosed || buf.position() == 0) {
			outKey.interestOps(outKey.interestOps() & ~SelectionKey.OP_WRITE);
		} else {
			outKey.interestOps(outKey.interestOps() | SelectionKey.OP_WRITE);
		}
		
		if (buf.hasRemaining() && !inClosed) {
			inKey.interestOps(inKey.interestOps() | SelectionKey.OP_READ);
		}					
	}
	
	
	void flushAndClose() {
		log.finest("server has closed, we should shutdown read");
		try {
			((SocketChannel)inKey.channel()).shutdownInput();
		} catch (IOException ignored) {
				
		}
		inClosed = true;
		if (buf.position() == 0) {
			outClosed = true;
		}
	}
}
