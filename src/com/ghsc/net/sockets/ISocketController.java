package com.ghsc.net.sockets;

import java.io.IOException;

/**
 * Base-interface for all socket controller/listeners.
 * @author Odell
 */
public interface ISocketController {
	
	/**
	 * Starts this socket controller/listener.
	 */
	public void start();
	/**
	 * Closes this socket controller/listener.
	 * @throws IOException
	 */
	public void close() throws IOException;
	
}