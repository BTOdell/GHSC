package com.ghsc.gui.components.button;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractButton;

/**
 * Will always ensure that a button in this group is selected.
 * @author Odell
 */
public class SelectedButtonGroup {
	
	private final ItemListener iL = new ItemListener() {
		public void itemStateChanged(ItemEvent ie) {
			Object src = ie.getSource();
			if (src == null || !(src instanceof AbstractButton)) {
                return;
            }
			final AbstractButton ab = (AbstractButton) src;
			if (SelectedButtonGroup.this.getSelected().length < 1) {
				ab.setSelected(true);
			}
		}
	};
	
	protected Vector<AbstractButton> buttons;
	
	public SelectedButtonGroup() {
		this.buttons = new Vector<AbstractButton>();
	}
	
	public SelectedButtonGroup(final int buttonCount) {
		this.buttons = new Vector<AbstractButton>(buttonCount);
	}
	
	public boolean add(final AbstractButton button) {
		if (this.buttons.size() < 1 && !button.isSelected()) {
            button.setSelected(true);
        }
		button.addItemListener(this.iL);
		return this.buttons.add(button);
	}
	
	public int getButtonCount() {
		return this.buttons.size();
	}
	
	public Enumeration<AbstractButton> getElements() {
		return this.buttons.elements();
	}
	
	public AbstractButton[] getSelected() {
		final ArrayList<AbstractButton> temp = new ArrayList<AbstractButton>(this.getButtonCount());
		final Enumeration<AbstractButton> enumeration = this.getElements();
		while (enumeration.hasMoreElements()) {
			final AbstractButton ab = enumeration.nextElement();
			if (ab != null && ab.isSelected()) {
                temp.add(ab);
            }
		}
		return temp.toArray(new AbstractButton[temp.size()]);
	}
	
	public boolean remove(final AbstractButton button) {
		button.removeItemListener(this.iL);
		return this.buttons.remove(button);
	}
	
}