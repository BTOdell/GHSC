package com.ghsc;

import com.ghsc.files.Profile;
import com.ghsc.files.Settings;
import com.ghsc.gui.Application;

/**
 * Boots up the application.
 */
public class Boot {

	/**
	 * The main starting point of application.
	 * @param args - any parameters to pass?
	 */
	public static void main(String[] args) {
		final Application application = Application.getInstance();
		System.out.println("Starting up...");
		try {
			System.setSecurityManager(null);
		} catch (SecurityException se) {}
		Profile.getProfile();
		Settings settings = Settings.getSettings();
		System.out.println("GHSC folder: " + settings.getStorageDirectory());
		try {
			application.initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}