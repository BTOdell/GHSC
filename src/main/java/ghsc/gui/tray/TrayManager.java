package ghsc.gui.tray;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import ghsc.common.Images;
import ghsc.gui.Application;

/**
 * Controls the tray icon and all things that have to do with it.
 */
public class TrayManager {
	
	private String tooltip = "GHSC";
	private final TrayIcon icon;

	private boolean msgStillRunning = true;
	
	/**
	 * Initializes a new TrayManager.
	 * @param tooltip A tooltip to place on the tray icon.
	 */
	public TrayManager(final String tooltip) {
		if (!isSupported()) {
            throw new UnsupportedOperationException("System tray is unavailable to this platform.");
        }
		this.icon = new TrayIcon(Images.ICON_16, tooltip == null ? this.tooltip : (this.tooltip = tooltip), new TrayPopup());
		this.icon.setImageAutoSize(true);
		this.icon.addMouseListener(new MouseAdapter() {
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() > 1) {
					Application.getInstance().getMainFrame().setVisible(true);
				}
			}
		});
	}
	
	/**
	 * Changes the tray icon tooltip.
	 * @param t - the text to set as tooltip.
	 */
	public void updateTooltip(String t) {
		if (t == null) {
            t = "";
        }
		this.tooltip = t;
		if (this.icon != null) {
			this.icon.setToolTip(t);
        }
	}
	
	public void onFrameClosed() {
		if (this.msgStillRunning) {
			this.showInfoMessage("GHSC is still running in the background.\nRight click this icon and press 'Exit' to terminate this application.");
			this.msgStillRunning = false;
		}
	}
	
	/*
	 * Message alerts
	 */
	
	public void showInfoMessage(final String text) {
		this.showInfoMessage(null, text);
	}
	
	public void showInfoMessage(final String caption, final String text) {
		this.icon.displayMessage(caption, text, MessageType.INFO);
	}
	
	public void showErrorMessage(final String text) {
		this.showInfoMessage(null, text);
	}
	
	public void showErrorMessage(final String caption, final String text) {
		this.icon.displayMessage(caption, text, MessageType.ERROR);
	}
	
	public void showWarningMessage(final String text) {
		this.showInfoMessage(null, text);
	}
	
	public void showWarningMessage(final String caption, final String text) {
		this.icon.displayMessage(caption, text, MessageType.WARNING);
	}
	
	/*
	 * End of message alert functions.
	 */
	
	/**
	 * Shows the tray icon.
	 */
	public boolean activate() {
		try {
			SystemTray.getSystemTray().add(this.icon);
		} catch (final Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Hides the tray icon.
	 */
	public boolean deactivate() {
		try {
			SystemTray.getSystemTray().remove(this.icon);
		} catch (final Exception e) {
			return false;
		}
		return true;
	}
	
	public static boolean isSupported() {
		return SystemTray.isSupported();
	}
	
}