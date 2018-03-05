package com.ghsc.gui.components.popup;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

/**
 * Controls all popups that appear for the application.
 * Utilizing only one Popup object, this manager ensures only a small amount of memory is used for all popups.
 */
public class PopupManager {
	
	private final Popup menu;
	private final HashMap<Component, MouseAdapter> adapterMap;
	
	/**
	 * Initializes a new PopupManager.
	 */
	public PopupManager() {
		this.menu = new Popup();
		this.adapterMap = new HashMap<>();
	}
	
	/**
	 * Submits the given component to this PopupManager and supplies it with a PopupBuilder to construct it.
	 * @param builder The builder used to construct the popup.
	 * @param comps The component(s) to submit.
	 */
	public void submit(final PopupBuilder builder, final Component... comps) {
		final MouseAdapter a = new MouseAdapter() {
			public void mousePressed(final MouseEvent e) {
				PopupManager.this.tryPopup(e, e.getComponent(), builder, e.getX(), e.getY());
			}
			public void mouseReleased(final MouseEvent e) {
				PopupManager.this.tryPopup(e, e.getComponent(), builder, e.getX(), e.getY());
			}
		};
		for (final Component comp : comps) {
			this.adapterMap.put(comp, a);
			comp.addMouseListener(a);
		}
	}
	
	/**
	 * Removes any popup association from this component.
	 * @param comp - the component to remove popup association from.
	 */
	public void remove(final Component comp) {
		comp.removeMouseListener(this.adapterMap.get(comp));
	}
	
	/**
	 * Decides whether a popup should be displayed on the given component, and if so, shows the popup at the given x/y.
	 */
	private void tryPopup(final MouseEvent e,
						  final Component comp,
						  final PopupBuilder builder, final int x, final int y) {
		if (e.isPopupTrigger()) {
			this.menu.removeAll();
			if (builder.build(this.menu, this, comp, x, y)) {
				this.menu.show(comp, x, y);
            }
		}
	}
	
}