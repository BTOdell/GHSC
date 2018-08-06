package ghsc.impl;

import java.util.UUID;

/**
 * Provides identification for all users.
 */
public interface Identifiable {

	/**
	 * Gets the nickname of this Identifiable.
	 */
	String getNick();

	/**
     * Gets the unique ID (UUID) of this Identifiable.
	 */
	UUID getID();

}