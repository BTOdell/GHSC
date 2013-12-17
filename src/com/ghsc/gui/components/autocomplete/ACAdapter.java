package com.ghsc.gui.components.autocomplete;

import javax.swing.text.JTextComponent;

public abstract class ACAdapter {
	
	private String selectedItemString;
	
	public abstract Object getSelectedItem();

	public abstract void setSelectedItem(Object item);
	
	public String getSelectedItemString() {
		return selectedItemString;
	}
	
	public void setSelectedItemString(String itemStr) {
		this.selectedItemString = itemStr;
	}
	
	public abstract int getItemCount();
	
	public abstract Object getItem(int index);
	
	public boolean listContainsSelectedItem() {
		Object selectedItem = getSelectedItem();
		for (int i = 0, n = getItemCount(); i < n; i++) {
			if (getItem(i) == selectedItem) 
				return true;
		}
		return false;
	}
	
	public abstract JTextComponent getTextComponent();

	public void markAll() {
		markFrom(0);
	}
	
	public void markFrom(int start) {
		getTextComponent().setCaretPosition(getTextComponent().getText().length());
		getTextComponent().moveCaretPosition(start);
	}
	
}