package com.ghsc.gui.components.autocomplete;

import java.util.List;

import javax.swing.text.JTextComponent;

public class TextComponentAdapter extends ACAdapter {
    
    List<?> items;
    JTextComponent textComponent;
    Object selectedItem;
    
    public TextComponentAdapter(final JTextComponent textComponent, final List<?> items) {
        this.items = items;
        this.textComponent = textComponent;
    }
    
    @Override
    public Object getSelectedItem() {
        return this.selectedItem;
    }
    
    @Override
    public int getItemCount() {
        return this.items.size();
    }
    
    @Override
    public Object getItem(final int index) {
        return this.items.get(index);
    }
    
    @Override
    public void setSelectedItem(final Object item) {
        this.selectedItem = item;
    }
    
    @Override
    public JTextComponent getTextComponent() {
        return this.textComponent;
    }
    
}