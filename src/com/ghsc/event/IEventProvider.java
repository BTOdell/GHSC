package com.ghsc.event;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public interface IEventProvider<E> {
	public boolean subscribe(final EventListener<E> listener);
	public boolean unsubscribe(final EventListener<E> listener);
}