package com.ghsc.gui.components.autocomplete;

import java.util.List;

import javax.swing.text.JTextComponent;

public class TextComponentAdapter extends ACAdapter {
    
    List<?> items;
    JTextComponent textComponent;
    Object selectedItem;
    
    public TextComponentAdapter(JTextComponent textComponent, List<?> items) {
        this.items = items;
        this.textComponent = textComponent;
    }
    
    @Override
    public Object getSelectedItem() {
        return selectedItem;
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    @Override
    public Object getItem(int index) {
        return items.get(index);
    }
    
    @Override
    public void setSelectedItem(Object item) {
        selectedItem = item;
    }
    
    @Override
    public JTextComponent getTextComponent() {
        return textComponent;
    }
    
}