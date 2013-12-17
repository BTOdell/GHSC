package com.ghsc.impl;

import java.util.UUID;

/**
 * Provides identification for all users.
 * @author Odell
 */
public interface Identifiable {
	/**
	 * @return the nickname of this Identifiable.
	 */
	public String getNick();
	/**
	 * @return the unique id (UUID) of this Identifiable.
	 */
	public UUID getID();
}