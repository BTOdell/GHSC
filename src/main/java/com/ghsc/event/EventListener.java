package com.ghsc.event;

/**
 * 
 */
public interface EventListener<E> {
	
	/**
	 * Occurs when a provider fires an event to its listeners.
	 * @param event the event object received.
	 */
	void eventReceived(E event);
	
}