package ghsc.gui.components.popup;

import java.awt.Component;

/**
 * Used to provide dynamic instructions to populate a Popup menu.
 * @author Odell
 */
public interface PopupBuilder {
	/**
	 * Instructions to populate a Popup menu.
	 */
	boolean build(Popup menu, PopupManager popupManager, Component sender, int x, int y);
}