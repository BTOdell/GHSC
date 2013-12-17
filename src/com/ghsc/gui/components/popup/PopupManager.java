package com.ghsc.gui.components.popup;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

/**
 * Controls all popups that appear for GHSC.</br>
 * Utilizing only one Popup object, this manager ensures only a small amount of memory is used for all popups.
 * @author Odell
 */
public class PopupManager {
	
	private Popup menu;
	private HashMap<Component, MouseAdapter> adapterMap;
	
	/**
	 * Initializes a new PopupManager.
	 */
	public PopupManager() {
		menu = new Popup();
		adapterMap = new HashMap<Component, MouseAdapter>();
	}
	
	/**
	 * Submits the given component to this PopupManager and supplies it with a PopupBuilder to construct it.
	 * @param comp - the component to submit.
	 * @param builder - the builder used to construct the popup.
	 */
	public void submit(final PopupBuilder builder, final Component... comps) {
		MouseAdapter a = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				tryPopup(e, e.getComponent(), builder, e.getX(), e.getY());
			}
			public void mouseReleased(MouseEvent e) {
				tryPopup(e, e.getComponent(), builder, e.getX(), e.getY());
			}
		};
		for (Component comp : comps) {
			adapterMap.put(comp, a);
			comp.addMouseListener(a);
		}
	}
	
	/**
	 * Removes any popup association from this component.
	 * @param comp - the component to remove popup association from.
	 */
	public void remove(final Component comp) {
		comp.removeMouseListener(adapterMap.get(comp));
	}
	
	/**
	 * Decides whether a popup should be displayed on the given component, and if so, shows the popup at the given x/y.
	 */
	private void tryPopup(MouseEvent e, Component comp, PopupBuilder builder, int x, int y) {
		if (e.isPopupTrigger()) {
			menu.removeAll();
			if (builder.build(menu, PopupManager.this, comp, x, y))
				menu.show(comp, x, y);
		}
	}
	
}