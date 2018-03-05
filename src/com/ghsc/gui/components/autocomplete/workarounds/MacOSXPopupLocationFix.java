package com.ghsc.gui.components.autocomplete.workarounds;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

@SuppressWarnings("rawtypes")
public final class MacOSXPopupLocationFix {
	
	private final JComboBox comboBox;
	private final JPopupMenu popupMenu;
	private final Listener listener = new Listener();

	/**
	 * Private constructor so users use the more action-oriented {@link #install} method.
	 */
	private MacOSXPopupLocationFix(JComboBox comboBox) {
		this.comboBox = comboBox;
		this.popupMenu = (JPopupMenu) comboBox.getUI().getAccessibleChild(comboBox, 0);
		this.popupMenu.addPopupMenuListener(this.listener);
	}

	/**
	 * Install the fix for the specified combo box.
	 */
	public static MacOSXPopupLocationFix install(JComboBox comboBox) {
		if (comboBox == null) {
			throw new IllegalArgumentException("combobox can't be null.");
		}
		return new MacOSXPopupLocationFix(comboBox);
	}

	/**
	 * Uninstall the fix. Usually this is unnecessary since letting the combo box go out of scope is sufficient.
	 */
	public void uninstall() {
		this.popupMenu.removePopupMenuListener(this.listener);
	}

	/**
	 * Reposition the popup immediately before it is shown.
	 */
	private class Listener implements PopupMenuListener {
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			final JComponent popupComponent = (JComponent) e.getSource();
			MacOSXPopupLocationFix.this.fixPopupLocation(popupComponent);
		}
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
		public void popupMenuCanceled(PopupMenuEvent e) {}
	}

	/**
	 * Do the adjustment on the specified popupComponent immediately before it is displayed.
	 */
	private void fixPopupLocation(JComponent popupComponent) {
		// we only need to fix Apple's aqua look and feel
		if (popupComponent.getClass().getName().indexOf("apple.laf") != 0) {
			return;
		}
		// put the popup right under the combo box so it looks like a
		// normal Aqua combo box
		Point comboLocationOnScreen = this.comboBox.getLocationOnScreen();
		int comboHeight = this.comboBox.getHeight();
		int popupY = comboLocationOnScreen.y + comboHeight;
		// ...unless the popup overflows the screen, in which case we put it
		// above the combobox
		Rectangle screenBounds = new ScreenGeometry(this.comboBox).getScreenBounds();
		int popupHeight = popupComponent.getPreferredSize().height;
		if (comboLocationOnScreen.y + comboHeight + popupHeight > screenBounds.x + screenBounds.height) {
			popupY = comboLocationOnScreen.y - popupHeight;
		}
		popupComponent.setLocation(comboLocationOnScreen.x, popupY);
	}

	/**
	 * Figure out the dimensions of our screen.
	 * <p>
	 * This code is inspired by similar in <code>JPopupMenu.adjustPopupLocationToFitScreen()</code>.
	 * 
	 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
	 */
	private final static class ScreenGeometry {
		
		final GraphicsConfiguration graphicsConfiguration;
		final boolean aqua;

		public ScreenGeometry(JComponent component) {
			this.aqua = UIManager.getLookAndFeel().getName().contains("Aqua");
			this.graphicsConfiguration = this.graphicsConfigurationForComponent(component);
		}

		/**
		 * Get the best graphics configuration for the specified point and component.
		 */
		private GraphicsConfiguration graphicsConfigurationForComponent(final Component component) {
			final Point point = component.getLocationOnScreen();
			// try to find the graphics configuration for our point of interest
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			for (GraphicsDevice aGd : gd) {
				if (aGd.getType() != GraphicsDevice.TYPE_RASTER_SCREEN) {
					continue;
				}
				GraphicsConfiguration defaultGraphicsConfiguration = aGd.getDefaultConfiguration();
				if (!defaultGraphicsConfiguration.getBounds().contains(point)) {
					continue;
				}
				return defaultGraphicsConfiguration;
			}
			// we couldn't find a graphics configuration, use the component's
			return component.getGraphicsConfiguration();
		}

		/**
		 * Get the bounds of where we can put a popup.
		 */
		public Rectangle getScreenBounds() {
			Rectangle screenSize = this.getScreenSize();
			Insets screenInsets = this.getScreenInsets();
			return new Rectangle(screenSize.x + screenInsets.left, screenSize.y + screenInsets.top, screenSize.width - screenInsets.left - screenInsets.right, screenSize.height - screenInsets.top - screenInsets.bottom);
		}

		/**
		 * Get the bounds of the screen currently displaying the component.
		 */
		public Rectangle getScreenSize() {
			// get the screen bounds and insets via the graphics configuration
			if (this.graphicsConfiguration != null) {
				return this.graphicsConfiguration.getBounds();
			}
			// just use the toolkit bounds, it's less awesome but sufficient
			return new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		}

		/**
		 * Fetch the screen insets, the off limits areas around the screen such as menu bar, dock or start bar.
		 */
		public Insets getScreenInsets() {
			Insets screenInsets;
			if (this.graphicsConfiguration != null) {
				screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(this.graphicsConfiguration);
			} else {
				screenInsets = new Insets(0, 0, 0, 0);
			}
			// tweak the insets for aqua, they're reported incorrectly there
			if (this.aqua) {
				int aquaBottomInsets = 21; // unreported insets, shown in screenshot, https://glazedlists.dev.java.net/issues/show_bug.cgi?id=332
				int aquaTopInsets = 22; // for Apple menu bar, found via debugger
				screenInsets.bottom = Math.max(screenInsets.bottom, aquaBottomInsets);
				screenInsets.top = Math.max(screenInsets.top, aquaTopInsets);
			}
			return screenInsets;
		}
		
	}
	
}