package com.ghsc.net.sockets;

/**
 * Base-interface for all socket controller/listeners.
 */
public interface ISocketController {
	
	/**
	 * Starts this socket controller/listener.
	 */
    void start();

	/**
	 * Closes this socket controller/listener.
	 */
    void close();
	
}