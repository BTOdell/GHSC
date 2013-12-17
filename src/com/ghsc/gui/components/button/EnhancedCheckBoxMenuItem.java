package com.ghsc.gui.components.button;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An extension of JCheckBoxMenuItem that doesn't close the menu when selected.
 * @author Odell
 */
public class EnhancedCheckBoxMenuItem extends JCheckBoxMenuItem {
	
	private static final long serialVersionUID = 1L;
	
	private static MenuElement[] path;
	
	private final ChangeListener cl = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			if (getModel().isArmed() && isShowing()) {
				path = MenuSelectionManager.defaultManager().getSelectedPath();
			}
		}
	};
	
	private boolean stayOpen = false;

	public EnhancedCheckBoxMenuItem() {
		super();
	}
	
	public EnhancedCheckBoxMenuItem(boolean stayOpen) {
		super();
		setStayOpen(stayOpen);
	}
	
	public EnhancedCheckBoxMenuItem(String text) {
		super(text);
	}
	
	public EnhancedCheckBoxMenuItem(String text, boolean stayOpen) {
		super(text);
		setStayOpen(stayOpen);
	}
	
	public boolean isStayOpen() {
		return stayOpen;
	}
	
	public void setStayOpen(boolean enabled) {
		if (stayOpen != enabled) { // state will change
			this.stayOpen = enabled;
			if (enabled) {
				getModel().addChangeListener(cl);
			} else {
				getModel().removeChangeListener(cl);
			}
		}
	}

	/**
	 * Overridden to reopen the menu.
	 * 
	 * @param pressTime
	 *            the time to "hold down" the button, in milliseconds
	 */
	@Override
	public void doClick(int pressTime) {
		super.doClick(pressTime);
		if (path.length > 0) {
			MenuElement source = path[0];
			if (source != null && source instanceof JPopupMenu) {
				JPopupMenu jpm = (JPopupMenu) source;
				jpm.setVisible(true);
			}
		}
		MenuSelectionManager.defaultManager().setSelectedPath(path);
	}
	
}