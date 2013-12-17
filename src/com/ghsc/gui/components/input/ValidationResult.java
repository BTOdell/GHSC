package com.ghsc.gui.components.input;

/**
 * Creates a wrapper which allows a validator to return both a boolean result as well as a string tooltip.
 * @author Odell
 */
public class ValidationResult<V,R> {
	
	private final V value;
	private final R result;
	
	/**
	 * Initializes a new ValidationResult with a value and a result.
	 */
	public ValidationResult(V value, R result) {
		this.value = value;
		this.result = result;
	}
	
	public final V getValue() {
		return value;
	}
	
	public final R getResult() {
		return result;
	}
	
}