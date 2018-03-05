package com.ghsc.gui.components.autocomplete;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
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
	public void valueChanged(ListSelectionEvent listSelectionEvent) {
		this.getTextComponent().setText(this.stringConverter.getPreferredStringForItem(this.list.getSelectedValue()));
		this.markAll();
	}

	@Override
	public Object getSelectedItem() {
		return this.list.getSelectedValue();
	}

	@Override
	public int getItemCount() {
		return this.list.getModel().getSize();
	}

	@Override
	public Object getItem(int index) {
		return this.list.getModel().getElementAt(index);
	}

	@Override
	public void setSelectedItem(Object item) {
        this.list.setSelectedValue(item, true);
	}

	@Override
	public JTextComponent getTextComponent() {
		return this.textComponent;
	}
	
}