package ghsc.gui.components.input;

/**
 * Creates a wrapper which allows a validator to return both a boolean result as well as a string tooltip.
 */
public class ValidationResult<V,R> {
	
	private final V value;
	private final R result;
	
	/**
	 * Initializes a new ValidationResult with a value and a result.
	 */
	public ValidationResult(final V value, final R result) {
		this.value = value;
		this.result = result;
	}
	
	public final V getValue() {
		return this.value;
	}
	
	public final R getResult() {
		return this.result;
	}
	
}