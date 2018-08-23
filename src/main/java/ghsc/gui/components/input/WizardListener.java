package ghsc.gui.components.input;

/**
 * A listener for when a wizard user interface finishes a produces a result object.
 */
public interface WizardListener<I> {
	void wizardFinished(I obj);
}