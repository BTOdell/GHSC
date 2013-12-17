package com.ghsc.event;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public interface EventListener<E> {
	/**
	 * Occurs when a provider fires an event to its listeners.
	 * @param event the event object received.
	 */
	public void eventReceived(E event);
}