package com.ghsc.gui.components.autocomplete;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;
import java.awt.*;

@SuppressWarnings("rawtypes")
public class ComboBoxAdapter extends ACAdapter {
	
	private final JComboBox comboBox;
	
	public ComboBoxAdapter(final JComboBox comboBox) {
		this.comboBox = comboBox;
		comboBox.addActionListener(arg0 -> this.markAll());
	}

	@Override
	public int getItemCount() {
		return this.comboBox.getItemCount();
	}

	@Override
	public Object getItem(final int index) {
		return this.comboBox.getItemAt(index);
	}

	@Override
	public void setSelectedItem(final Object item) {
		if (item == this.getSelectedItem()) {
			return;
		}
		final Accessible a = this.comboBox.getUI().getAccessibleChild(this.comboBox, 0);
		if (this.getItemCount() > 0 && a instanceof ComboPopup) {
			final JList list = ((ComboPopup) a).getList();
			final int lastIndex = list.getModel().getSize() - 1;
			final Rectangle rect = list.getCellBounds(lastIndex, lastIndex);
			if (rect == null) {
				throw new IllegalStateException("failed to access index [" + lastIndex + "] for " + this.comboBox);
			}
			list.scrollRectToVisible(rect);
		}
        this.comboBox.setSelectedItem(item);
	}

	@Override
	public Object getSelectedItem() {
		return this.comboBox.getModel().getSelectedItem();
	}

	@Override
	public JTextComponent getTextComponent() {
		return (JTextComponent) this.comboBox.getEditor().getEditorComponent();
	}
	
}