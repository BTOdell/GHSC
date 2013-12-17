package com.ghsc.gui.tray;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.ghsc.common.Images;
import com.ghsc.gui.Application;

/**
 * Controls the tray icon and all things that have to do with it.
 * @author Odell
 */
public class TrayManager {
	
	private final Application application;
	
	private String tooltip = "GHSC";
	private TrayIcon icon;
	private TrayPopup popup;
	
	private boolean msgStillRunning = true;
	
	/**
	 * Initializes a new TrayManager.
	 * @param tooltip - a tooltip to place on the tray icon.
	 */
	public TrayManager(String tooltip) {
		if (!isSupported())
			throw new UnsupportedOperationException("System tray is unavailable to this platform.");
		this.application = Application.getApplication();
		popup = new TrayPopup();
		icon = new TrayIcon(Images.ICON_16, tooltip == null ? this.tooltip : (this.tooltip = tooltip), popup);
		icon.setImageAutoSize(true);
		icon.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					application.getMainFrame().setVisible(true);
				}
			}
		});
	}
	
	/**
	 * Changes the tray icon tooltip.
	 * @param t - the text to set as tooltip.
	 */
	public void updateTooltip(String t) {
		if (t == null)
			t = "";
		tooltip = t;
		if (icon != null)
			icon.setToolTip(t);
	}
	
	public void onFrameClosed() {
		if (msgStillRunning) {
			showInfoMessage("GHSC is still running in the background.\nRight click this icon and press 'Exit' to terminate this application.");
			msgStillRunning = false;
		}
	}
	
	/*
	 * Message alerts
	 */
	
	public void showInfoMessage(String text) {
		showInfoMessage(null, text);
	}
	
	public void showInfoMessage(String caption, String text) {
		icon.displayMessage(caption, text, MessageType.INFO);
	}
	
	public void showErrorMessage(String text) {
		showInfoMessage(null, text);
	}
	
	public void showErrorMessage(String caption, String text) {
		icon.displayMessage(caption, text, MessageType.ERROR);
	}
	
	public void showWarningMessage(String text) {
		showInfoMessage(null, text);
	}
	
	public void showWarningMessage(String caption, String text) {
		icon.displayMessage(caption, text, MessageType.WARNING);
	}
	
	/*
	 * End of message alert functions.
	 */
	
	/**
	 * Shows the tray icon.
	 */
	public boolean activate() {
		try {
			SystemTray.getSystemTray().add(icon);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Hides the tray icon.
	 */
	public boolean deactivate() {
		try {
			SystemTray.getSystemTray().remove(icon);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static boolean isSupported() {
		return SystemTray.isSupported();
	}
	
}