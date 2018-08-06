package com.ghsc.impl;

import java.util.UUID;

/**
 * Provides complex identification for all users.
 * @author Odell
 */
public interface ComplexIdentifiable extends Identifiable {

	/**
	 * @return the host name of this Identifiable.
	 */
	String getHostname();

	/**
	 * Sets a new hostname for this Identifiable.
	 * @param hostname the new hostname.
	 */
	void setHostname(String hostname);

	/**
	 * Sets a new nickname for this Identifiable.
	 * @param nick the new nickname.
	 */
	void setNick(String nick);

	/**
	 * Picks between the hostname and the nick.
	 * @return the hostname only if the nick is invalid.
	 */
	String getPreferredName();

	/**
	 * Directly sets the unique id of this Identifiable.
	 * @param uuid the new UUID.
	 */
	void setID(UUID uuid);

	/**
	 * Attempts to parse the given uuid string and set it as the id.
	 * @param uuid a String to parse a UUID.
	 */
	void setID(String uuid);

}