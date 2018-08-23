package ghsc.gui.components.input;

/**
 * A function that validates input to a wizard user interface.
 */
public interface WizardValidator<I, V, R> {
	ValidationResult<V, R> validate(I text);
}