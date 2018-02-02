package oss.chapman.proxy;

import java.util.ArrayDeque;
import java.util.logging.Level;

public class SimpleProxyConfig {

	String listenHost;
	int    listenPort;
	
	String targetHost;
	int    targetPort;
	
	int    idleTimeout_sec;
	int    statisticsInterval_sec;
	Level statisticsLoggingLevel;
	
	
	protected SimpleProxyConfig()
	{
		
	}
	
	
	private static void usage() 
	{
		System.out.println("Usage: java -jar SimpleProxy.jar {ListenIP} {ListenPort} {TargetHost} {TargetPort}");
		System.exit(2);
	}
	/**
	 * Parse the command-line args and return the configuration.
	 * @param argv  -- command line from main()
	 */
	public static SimpleProxyConfig fromArgs(String[] args) {
		SimpleProxyConfig config = new SimpleProxyConfig();

		if (args.length != 4) {
			usage();
		}
		
		
		config.listenHost = args[0];
		config.listenPort = Integer.parseInt(args[1]);
		config.targetHost = args[2];
		config.targetPort = Integer.parseInt(args[3]);
		config.idleTimeout_sec=10;
		config.statisticsInterval_sec= 30*60; // every 30 minutes.
		config.statisticsLoggingLevel = Level.INFO;
		
		return config;
		
	}
	
}
