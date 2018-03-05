package com.ghsc.net.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JOptionPane;

import com.ghsc.common.Paths;
import com.ghsc.gui.Application;

/**
 * The updater provides seemless application updating.</br>
 * Near instant updates with high-end streamed downloading software.
 * @author Odell
 */
public class Updater {
	
	private static final int DOWNLOAD_BUFFER_SIZE = 8192;
	
	private Version version;
	
	/**
	 * Initializes a new Updater.
	 */
	public Updater() {
		this.version = Application.VERSION;
	}
	
	/**
	 * Using {@link #version}, this function creates a String representing the theoretical URL of the latest JAR file.</br>
	 * Will lookup the latest version if {@link #version} isn't initialized yet.
	 * @return a String URL path to the latest JAR file.
	 */
	public String getLatestJarPath() {
		return Paths.WEBHOST + this.version + "/GHSC.jar";
	}
	
	/**
	 * @return the latest known version of GHSC.
	 */
	public Version getVersion() {
		return this.version;
	}
	
	/**
	 * Downloads update (overwriting the current running version), then restarts the application to apply changes.
	 */
	private void update() {
		final Application application = Application.getInstance();
		application.getMainFrame().setStatus("Update found. Connecting...");
		// begin updating...
		try {
			final String currentRunningPath = Application.currentRunningPath();
			FileOutputStream out = null;
			InputStream in = null;
			try {
				//stream latest version to file system
				out = new FileOutputStream(new File(currentRunningPath), false);
				in = new URL(this.getLatestJarPath()).openStream();
				application.getMainFrame().setStatus("Update found. Downloading...");
				final byte[] buf = new byte[DOWNLOAD_BUFFER_SIZE];
				int read;
				while ((read = in.read(buf)) >= 0) {
					out.write(buf, 0, read);
				}
			} finally {
				if (out != null) {
					try {
						out.flush();
						out.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
				if (in != null) {
					try {
						in.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
			// restart application
			application.getMainFrame().setStatus("Restarting...");
			Application.restart();
		} catch (final Exception e) {
			e.printStackTrace();
			// cancel update...
			JOptionPane.showMessageDialog(null, "Updating failed. Contact developer and report this issue.", "Update failed.", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Checks for an update.</br>
	 * If it finds one, prompts the user to continue and then updates.
	 * @param debug - whether to print version status to the console.
	 */
	public void updateCheck(final boolean refresh, final boolean debug) {
		final Application application = Application.getInstance();
		final Version latestVersion = refresh ? application.getVersionController().refresh(false) : application.getVersionController().getLatest();
		if (latestVersion != null) {
			final int updateStatus = latestVersion.compareTo(this.version);
			if (updateStatus < 0) {
				if (debug) {
                    System.out.println("Application is out of date and needs update!");
                }
				if (Application.isJar()) {
					final boolean required = application.getVersionController().isRequired(latestVersion);
					final boolean forced = application.getVersionController().isForced(latestVersion);
					if (forced || JOptionPane.showOptionDialog(null, "An update has been found for GHSC.\nWould you like to update now?", "Update found!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] { "Update", required ? "Exit" : "Cancel" }, "Update") == 0) {
						this.version = latestVersion;
						this.update();
						return;
					}
					if (required) {
                        System.exit(0);
                    }
				} else {
					if (debug) {
                        System.out.println("Can't update, not running from jar.");
                    }
				}
			} else if (updateStatus == 0) {
				if (debug) {
                    System.out.println("Application is up to date!");
                }
			} else {
				if (debug) {
                    System.out.println("Wow, you have a prototype version!");
                }
			}
		}
	}
	
}