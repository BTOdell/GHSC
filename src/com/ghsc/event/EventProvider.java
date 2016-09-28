package com.ghsc.event;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 
 */
public class EventProvider<E> implements IEventProvider<E> {
	
	private final String name;
	private final CopyOnWriteArraySet<EventListener<E>> listeners = new CopyOnWriteArraySet<>();
	private final Context context;
	
	public EventProvider() {
		this(null);
	}
	
	public EventProvider(final String name) {
		this.name = name;
		this.context = new Context();
	}
	
	public EventProvider<E>.Context getContext() {
		return context;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public boolean subscribe(EventListener<E> listener) {
		return listener != null && listeners.add(listener);
	}

	@Override
	public boolean unsubscribe(EventListener<E> listener) {
		return listener != null && listeners.remove(listener);
	}
	
	public void fireEvent(final E event) {
		for (final EventListener<E> e : this.listeners) {
			if (e == null) {
				continue;
			}
			e.eventReceived(event);
		}
	}
	
	public class Context implements IEventProvider<E> {

		public String getName() {
			return EventProvider.this.getName();
		}

		@Override
		public boolean subscribe(EventListener<E> listener) {
			return EventProvider.this.subscribe(listener);
		}

		@Override
		public boolean unsubscribe(EventListener<E> listener) {
			return EventProvider.this.unsubscribe(listener);
		}
		
	}
	
}