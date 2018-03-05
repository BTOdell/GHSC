package com.ghsc.gui.components.autocomplete;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.accessibility.Accessible;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;

@SuppressWarnings("rawtypes")
public class ComboBoxAdapter extends ACAdapter {
	
	private JComboBox comboBox;
	
	public ComboBoxAdapter(JComboBox comboBox) {
		this.comboBox = comboBox;
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ComboBoxAdapter.this.markAll();
			}
		});
	}

	@Override
	public int getItemCount() {
		return this.comboBox.getItemCount();
	}

	@Override
	public Object getItem(int index) {
		return this.comboBox.getItemAt(index);
	}

	@Override
	public void setSelectedItem(Object item) {
		if (item == this.getSelectedItem()) {
			return;
		}
		Accessible a = this.comboBox.getUI().getAccessibleChild(this.comboBox, 0);
		if (this.getItemCount() > 0 && a instanceof ComboPopup) {
			JList list = ((ComboPopup) a).getList();
			int lastIndex = list.getModel().getSize() - 1;
			Rectangle rect = list.getCellBounds(lastIndex, lastIndex);
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