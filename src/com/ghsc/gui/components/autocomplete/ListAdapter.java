package com.ghsc.gui.components.autocomplete;

import javax.swing.JList;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

public class ListAdapter extends ACAdapter implements ListSelectionListener {
	
	JList<?> list;
	JTextComponent textComponent;
	ObjectToStringConverter stringConverter;

	public ListAdapter(JList<?> list, JTextComponent textComponent) {
		this(list, textComponent, ObjectToStringConverter.DEFAULT);
	}

	public ListAdapter(JList<?> list, JTextComponent textComponent, ObjectToStringConverter stringConverter) {
		this.list = list;
		this.textComponent = textComponent;
		this.stringConverter = stringConverter;
		
		list.addListSelectionListener(this);
	}

	@Override
	public void valueChanged(javax.swing.event.ListSelectionEvent listSelectionEvent) {
		getTextComponent().setText(stringConverter.getPreferredStringForItem(list.getSelectedValue()));
		markAll();
	}

	@Override
	public Object getSelectedItem() {
		return list.getSelectedValue();
	}

	@Override
	public int getItemCount() {
		return list.getModel().getSize();
	}

	@Override
	public Object getItem(int index) {
		return list.getModel().getElementAt(index);
	}

	@Override
	public void setSelectedItem(Object item) {
		list.setSelectedValue(item, true);
	}

	@Override
	public JTextComponent getTextComponent() {
		return textComponent;
	}
	
}