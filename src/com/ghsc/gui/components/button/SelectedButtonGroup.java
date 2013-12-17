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
			if (src == null || !(src instanceof AbstractButton))
				return;
			final AbstractButton ab = (AbstractButton) src;
			if (getSelected().length < 1) {
				ab.setSelected(true);
			}
		}
	};
	
	protected Vector<AbstractButton> buttons;
	
	public SelectedButtonGroup() {
		buttons = new Vector<AbstractButton>();
	}
	
	public SelectedButtonGroup(final int buttonCount) {
		buttons = new Vector<AbstractButton>(buttonCount);
	}
	
	public boolean add(final AbstractButton button) {
		if (buttons.size() < 1 && !button.isSelected())
			button.setSelected(true);
		button.addItemListener(iL);
		return buttons.add(button);
	}
	
	public int getButtonCount() {
		return buttons.size();
	}
	
	public Enumeration<AbstractButton> getElements() {
		return buttons.elements();
	}
	
	public AbstractButton[] getSelected() {
		final ArrayList<AbstractButton> temp = new ArrayList<AbstractButton>(getButtonCount());
		final Enumeration<AbstractButton> enumeration = getElements();
		while (enumeration.hasMoreElements()) {
			final AbstractButton ab = enumeration.nextElement();
			if (ab != null && ab.isSelected())
				temp.add(ab);
		}
		return temp.toArray(new AbstractButton[temp.size()]);
	}
	
	public boolean remove(final AbstractButton button) {
		button.removeItemListener(iL);
		return buttons.remove(button);
	}
	
}