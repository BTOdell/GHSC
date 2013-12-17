package com.ghsc.gui.components.input;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public interface WizardValidator<I, V, R> {
	public ValidationResult<V, R> validate(I text);
}