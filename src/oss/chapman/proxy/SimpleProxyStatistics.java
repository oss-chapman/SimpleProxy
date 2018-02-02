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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chapmma6
 *
 */
public class SimpleProxyStatistics {

	Logger log;
	Level level;
	
	long startTime;
	long statisticsInterval;
	long nextUpdate;
	
	long maxConnectionsLife;    // lifetime highwater, number of open connections at once.
	long maxConnectionsInterval; // interval highwater, number of open connections at once.
	long currConnections;        // current open connections
	long totalConnectionsLife;   // total number of connections over lifetime of program
	long totalConnectionsInterval; // total number of connections over the stats interval.
	
	long bytesToServerLife;     // total bytes written to the server, lifetime.
	long bytesToServerInterval; // total bytes written to server over the stats interval
	
	long bytesToClientLife;     // total bytes written to the client, lifetime
	long bytesToClientInterval; // total bytes written to the client, over the stats interval.
	
	
	long sumConnTimeLife;       // sum total connections over lifetime (used to calculate avg.)
	long sumConnTimeInterval;   // sum total connection time over the statistics interval.
	long maxConnTimeInterval;   // max single connection time over the interval.
	
	
	SimpleProxyStatistics(SimpleProxyConfig config)
	{
		startTime = System.currentTimeMillis();
		statisticsInterval = config.statisticsInterval_sec * 1000L;
		nextUpdate = 0L;
		log = Logger.getLogger("stats");
		level = config.statisticsLoggingLevel;
	}
	
	public void timerTick(long curr_time_ms) {
		if (curr_time_ms >=nextUpdate) {
			showStatistics(curr_time_ms);
			nextUpdate = curr_time_ms + statisticsInterval;
		}
	}	
	
	void showStatistics(long curr_time_ms) {
		java.lang.Runtime runtime = Runtime.getRuntime();
		StringBuilder v = new StringBuilder();
		v.append("======== Statistics ========");
		v.append("\n= Uptime: ").append((curr_time_ms - startTime)/ 1000L).append(" Seconds");
		v.append("\n= Memory: Free: ").append(runtime.freeMemory()).append(" Total: ").append(runtime.totalMemory()).append(" Max: ").append(runtime.maxMemory());
		v.append("\n= Connections:");
		v.append("\n=   Count");
		v.append("\n=    Max Lifetime: ").append(maxConnectionsLife).append(" Interval:").append(maxConnectionsInterval);
		v.append("\n=    Total Lifetime:").append(totalConnectionsLife);
		v.append("\n=    Current:").append(currConnections);
		maxConnectionsInterval = 0L;
		v.append("\n=  Timings:");
		long avg = 0L;
		if (totalConnectionsInterval != 0) {
			avg = sumConnTimeInterval/ totalConnectionsInterval;
		}
		v.append("\n=    Avg Connection Time Interval: ").append(avg).append(" ms");
		v.append("\n=    Max Connection Time Interval: ").append(maxConnTimeInterval).append(" ms");
		totalConnectionsInterval = 0L;
		sumConnTimeInterval = 0L;
		maxConnTimeInterval = 0L;
		v.append("\n= Network:");
		v.append("\n=   Bytes to Server:");
		v.append("\n=    Lifetime: ").append(bytesToServerLife).append(" Interval: ").append(bytesToServerInterval);
		bytesToServerInterval = 0L;
		v.append("\n=   Bytes to Client:");
		v.append("\n=    Lifetime: ").append(bytesToClientLife).append(" Interval: ").append(bytesToClientInterval);
		bytesToClientInterval = 0L;
		v.append("\n========\n");
		log.log(level,v.toString(),"stats");
	}
	
	public void connectionAdded(ProxyConnection c) {
		currConnections += 1;
		totalConnectionsInterval += 1;
		totalConnectionsLife += 1;
		if (currConnections > maxConnectionsInterval) {
			maxConnectionsInterval = currConnections;
		}
		if (currConnections > maxConnectionsLife) {
			maxConnectionsLife = currConnections;
		}
		
	}
	public void connectionRemoved(ProxyConnection c) {
		long curr_time_ms = System.currentTimeMillis();
		currConnections -= 1;
		
		long connTime = curr_time_ms - c.getStartTime();
		
		if (connTime > maxConnTimeInterval) {
			maxConnTimeInterval = connTime;
		}
		
		sumConnTimeInterval += connTime;
		sumConnTimeLife += connTime;
		
		
		long i = c.getBytesToServer();
		bytesToServerLife += i;
		bytesToServerInterval += i;
		
		i = c.getBytesToClient();
		bytesToClientLife += i;
		bytesToClientInterval += i;
	}
	
	
	
}
