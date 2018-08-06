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
		public void stateChanged(final ChangeEvent e) {
			if (EnhancedCheckBoxMenuItem.this.getModel().isArmed() && EnhancedCheckBoxMenuItem.this.isShowing()) {
				path = MenuSelectionManager.defaultManager().getSelectedPath();
			}
		}
	};
	
	private boolean stayOpen;

	public EnhancedCheckBoxMenuItem() {
		super();
	}
	
	public EnhancedCheckBoxMenuItem(final boolean stayOpen) {
		super();
		this.setStayOpen(stayOpen);
	}
	
	public EnhancedCheckBoxMenuItem(final String text) {
		super(text);
	}
	
	public EnhancedCheckBoxMenuItem(final String text, final boolean stayOpen) {
		super(text);
		this.setStayOpen(stayOpen);
	}
	
	public boolean isStayOpen() {
		return this.stayOpen;
	}
	
	public void setStayOpen(final boolean enabled) {
		if (this.stayOpen != enabled) { // state will change
			this.stayOpen = enabled;
			if (enabled) {
				this.getModel().addChangeListener(this.cl);
			} else {
				this.getModel().removeChangeListener(this.cl);
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
	public void doClick(final int pressTime) {
		super.doClick(pressTime);
		if (path.length > 0) {
			final MenuElement source = path[0];
			if (source instanceof JPopupMenu) {
				final JPopupMenu jpm = (JPopupMenu) source;
				jpm.setVisible(true);
			}
		}
		MenuSelectionManager.defaultManager().setSelectedPath(path);
	}
	
}