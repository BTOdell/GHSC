package ghsc.gui.components.input;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public interface WizardValidator<I, V, R> {
	ValidationResult<V, R> validate(I text);
}