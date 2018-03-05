package com.ghsc.event;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 
 */
public class EventProvider<E> implements IEventProvider<E> {
	
	private final CopyOnWriteArraySet<EventListener<E>> listeners = new CopyOnWriteArraySet<>();

	@Override
	public boolean subscribe(EventListener<E> listener) {
		return listener != null && this.listeners.add(listener);
	}

	@Override
	public boolean unsubscribe(EventListener<E> listener) {
		return listener != null && this.listeners.remove(listener);
	}

	/**
	 * Fire/dispatch the event to all the subscribed event listeners.
	 * @param event The event to dispatch.
	 */
	public void fireEvent(final E event) {
		for (final EventListener<E> e : this.listeners) {
			if (e == null) {
				continue;
			}
			e.eventReceived(event);
		}
	}
	
}