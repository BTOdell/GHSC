package com.ghsc.impl;

/**
 * An implementation of {@link Runnable} that provides a constructor with a generic parameter.
 * @author Odell
 * @param <G>
 * 		the generic object type.
 */
public abstract class GenericRunnable<G> implements Runnable {
	
	/**
	 * The generic object provided through the constructor of this GenericRunnable.
	 */
	protected G obj;
	
	/**
	 * Creates a new GenericRunnable with the given generic object.
	 * @param obj
	 * 		the generic object to store.
	 */
	public GenericRunnable(final G obj) {
		this.obj = obj;
	}
	
}