package com.ghsc.event.global;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

import com.ghsc.event.EventProvider;

/**
 * A utility to allow separate sectors of the application message each other through a publish-subscribe event system.<br>
 * 
 * Warning: A provider listener may receive more than one {@link EventProviderListener#providerAdded(EventProvider.Context)}
 * events if a listener and a provider are added at the same time.
 */
public class EventManager {
	
	private static EventManager instance = null;
	
	public static EventManager getEventManager() {
		if (instance == null) {
			instance = new EventManager();
		}
		return instance;
	}
	
	private final CopyOnWriteArraySet<EventProviderListener> providerListeners;
	private final CopyOnWriteArraySet<EventProvider<?>> providers;
	
	private EventManager() {
		providerListeners = new CopyOnWriteArraySet<>();
		providers = new CopyOnWriteArraySet<>();
	}
	
	/**
	 * Adds a provider listener to this manager.<br>
	 * Any existing providers will be automatically sent to this listener.
	 * @param epl the provider listener to add.
	 * @return whether this listener was added to the set of listeners.
	 */
	public boolean addListener(final EventProviderListener epl) {
		if (epl == null) {
			return false;
		}
		final Iterator<EventProvider<?>> it = providers.iterator();
		if (providerListeners.add(epl)) {
			while (it.hasNext()) {
				final EventProvider<?> p = it.next();
				if (p == null) {
					continue;
				}
				epl.providerAdded(p.getContext());
			}
			return true;
		}
		return false;
	}
	
	public boolean removeListener(final EventProviderListener epl) {
		return epl != null && providerListeners.remove(epl);
	}
	
	public boolean add(final EventProvider<?> ep) {
		if (ep == null)
			return false;
		final Iterator<EventProviderListener> it = providerListeners.iterator();
		if (providers.add(ep)) {
			while (it.hasNext()) {
				final EventProviderListener l = it.next();
				if (l == null) {
					continue;
				}
				l.providerAdded(ep.getContext());
			}
			return true;
		}
		return false;
	}
	
	public boolean remove(final EventProvider<?> ep) {
		if (ep == null) {
			return false;
		}
		if (providers.remove(ep)) {
			for (final EventProviderListener l : providerListeners) {
				if (l == null) {
					continue;
				}
				l.providerRemoved(ep.getContext());
			}
			return true;
		}
		return false;
	}
	
}