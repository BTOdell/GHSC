package com.ghsc.gui.components.autocomplete;

import javax.swing.text.JTextComponent;

public abstract class ACAdapter {
	
	private String selectedItemString;
	
	public abstract Object getSelectedItem();

	public abstract void setSelectedItem(Object item);
	
	public String getSelectedItemString() {
		return this.selectedItemString;
	}
	
	public void setSelectedItemString(final String itemStr) {
		this.selectedItemString = itemStr;
	}
	
	public abstract int getItemCount();
	
	public abstract Object getItem(int index);
	
	public boolean listContainsSelectedItem() {
		final Object selectedItem = this.getSelectedItem();
		for (int i = 0, n = this.getItemCount(); i < n; i++) {
			if (this.getItem(i) == selectedItem) {
                return true;
            }
		}
		return false;
	}
	
	public abstract JTextComponent getTextComponent();

	public void markAll() {
		this.markFrom(0);
	}
	
	public void markFrom(final int start) {
		this.getTextComponent().setCaretPosition(this.getTextComponent().getText().length());
		this.getTextComponent().moveCaretPosition(start);
	}
	
}