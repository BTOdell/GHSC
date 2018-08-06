package ghsc.gui.components.autocomplete;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxEditor;

public class ACComboBoxEditor implements ComboBoxEditor {
	
	final ComboBoxEditor wrapped;
	final ObjectToStringConverter converter;
	private Object lastSelected;
	
	public ACComboBoxEditor(final ComboBoxEditor wrapped, final ObjectToStringConverter converter) {
		this.wrapped = wrapped;
		this.converter = converter;
	}

	@Override
	public Component getEditorComponent() {
		return this.wrapped.getEditorComponent();
	}

	@Override
	public void setItem(final Object anObject) {
		this.lastSelected = anObject;
		this.wrapped.setItem(this.converter.getPreferredStringForItem(anObject));
	}

	@Override
	public Object getItem() {
		final Object wrappedItem = this.wrapped.getItem();
		final String[] oldAsStrings = this.converter.getPossibleStringsForItem(this.lastSelected);
		for (final String oldAsString : oldAsStrings) {
			if (oldAsString != null && oldAsString.equals(wrappedItem)) {
				return this.lastSelected;
			}
		}
		return null;
	}

	@Override
	public void selectAll() {
		this.wrapped.selectAll();
	}

	@Override
	public void addActionListener(final ActionListener l) {
		this.wrapped.addActionListener(l);
	}

	@Override
	public void removeActionListener(final ActionListener l) {
		this.wrapped.removeActionListener(l);
	}
	
}