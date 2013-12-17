package com.ghsc.event.global;

import com.ghsc.event.EventProvider;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public interface EventProviderListener {
	/**
	 * Occurs when an event provider is added to the EventManager.
	 * @param provider the provider added.
	 */
	void providerAdded(EventProvider<?>.Context context);
	/**
	 * Occurs when an event provider is removed from the EventManager.
	 * @param provider the provider removed.
	 */
	void providerRemoved(EventProvider<?>.Context context);
}