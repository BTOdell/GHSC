package com.ghsc.event;

/**
 * 
 */
public interface IEventProvider<E> {

	/**
	 * 
	 * @param listener
	 * @return
	 */
	boolean subscribe(final EventListener<E> listener);

	/**
	 * 
	 * @param listener
	 * @return
	 */
	boolean unsubscribe(final EventListener<E> listener);
	
}