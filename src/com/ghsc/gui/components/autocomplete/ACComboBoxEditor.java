package com.ghsc.gui.components.autocomplete;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxEditor;

public class ACComboBoxEditor implements ComboBoxEditor {
	
	final ComboBoxEditor wrapped;
	final ObjectToStringConverter converter;
	private Object lastSelected;
	
	public ACComboBoxEditor(ComboBoxEditor wrapped, ObjectToStringConverter converter) {
		this.wrapped = wrapped;
		this.converter = converter;
	}

	@Override
	public Component getEditorComponent() {
		return wrapped.getEditorComponent();
	}

	@Override
	public void setItem(Object anObject) {
		this.lastSelected = anObject;
		wrapped.setItem(converter.getPreferredStringForItem(anObject));
	}

	@Override
	public Object getItem() {
		final Object wrappedItem = wrapped.getItem();
		final String[] oldAsStrings = converter.getPossibleStringsForItem(lastSelected);
		for (int i = 0, n = oldAsStrings.length; i < n; i++) {
			final String oldAsString = oldAsStrings[i];
			if (oldAsString != null && oldAsString.equals(wrappedItem)) {
				return lastSelected;
			}
		}
		return null;
	}

	@Override
	public void selectAll() {
		wrapped.selectAll();
	}

	@Override
	public void addActionListener(ActionListener l) {
		wrapped.addActionListener(l);
	}

	@Override
	public void removeActionListener(ActionListener l) {
		wrapped.removeActionListener(l);
	}
	
}