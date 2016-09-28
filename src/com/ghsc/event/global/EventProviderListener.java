package com.ghsc.event.global;

import com.ghsc.event.EventProvider;

/**
 * 
 */
public interface EventProviderListener {
	
	/**
	 * Occurs when an event provider is added to the EventManager.
	 * @param context the provider context added.
	 */
	void providerAdded(final EventProvider<?>.Context context);
	
	/**
	 * Occurs when an event provider is removed from the EventManager.
	 * @param context the provider context removed.
	 */
	void providerRemoved(final EventProvider<?>.Context context);
	
}