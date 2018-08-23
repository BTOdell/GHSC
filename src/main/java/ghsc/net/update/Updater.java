package ghsc.net.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JOptionPane;

import ghsc.gui.Application;

/**
 * The updater provides seemless application updating.
 */
public class Updater {
	
	private static final int DOWNLOAD_BUFFER_SIZE = 8192;

	/**
	 * No instances!
	 */
	private Updater() {}

	/**
	 * Checks for an update.
	 * If it finds one, prompts the user to continue and then updates.
	 * @param debug Whether to print version status to the console.
	 */
	public static void updateCheck(final boolean debug) {
		final Application application = Application.getInstance();
		final Release latestRelease = application.getVersionController().getKnownLatest();
		if (latestRelease != null) {
			final int updateStatus = latestRelease.version.compareTo(Application.VERSION);
			if (updateStatus < 0) {
				if (debug) {
                    System.out.println("Application is out of date and needs update!");
                }
				if (Application.isJar()) {
				    final boolean required = latestRelease.isRequired();
				    final boolean forced = latestRelease.isForced();
					if (forced || JOptionPane.showOptionDialog(null, "An update has been found for GHSC.\nWould you like to update now?", "Update found!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] { "Update", required ? "Exit" : "Cancel" }, "Update") == 0) {
						update(latestRelease);
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

	/**
	 * Downloads release update (overwriting the current running version), then restarts the application to apply changes.
	 */
	private static void update(final Release release) {
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
                in = new URL(release.downloadURL).openStream();
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
	
}