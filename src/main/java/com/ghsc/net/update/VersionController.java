package com.ghsc.net.update;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

import com.ghsc.common.Paths;
import com.ghsc.gui.Application;
import com.ghsc.impl.Filter;

/**
 * Creates a controller to manage different versions of the application interacting with the running version.
 */
public class VersionController {
	
	private final int DELAY = 10000;
	
	private final ArrayList<Version> versions;
	private final Object versionSync = new Object();
	
	private Thread workThread;
	private final Runnable workRunnable = () -> {
        try {
            while (this.running) {
                Version lastLatest = this.getLatest();
                Version newLatest = this.refresh(false);
                if (!this.running) {
					break;
                }
                if (lastLatest.compareTo(newLatest) > 0) {
                    new Thread(() -> Application.getInstance().getUpdater().updateCheck(false, false)).start();
                }
                Thread.sleep(this.DELAY);
            }
        } catch (Exception ignored) {}
    };
	private boolean running = true;
	
	/**
	 * Initializes a new VersionController.
	 */
	public VersionController() {
		this.versions = new ArrayList<>();
	}
	
	/**
	 * Starts the threaded version monitor.
	 */
	public void start() {
		this.workThread = new Thread(this.workRunnable);
		this.workThread.setName("Version controller");
		this.running = true;
		this.workThread.start();
	}
	
	/**
	 * Stops the threaded version monitor.
	 */
	public void stop() {
		if (this.workThread != null && this.workThread.isAlive()) {
			this.running = false;
			this.workThread.interrupt();
		}
	}
	
	/**
	 * @return a sorted array of all the Versions currently loaded.
	 */
	private Version[] getAll() {
		final Version[] va = this.versions.toArray(new Version[0]);
		Arrays.sort(va);
		return va;
	}
	
	/**
	 * @return the latest known version of GHSC.
	 */
	public Version getLatest() {
		synchronized (this.versionSync) {
			final Version[] all = this.getAll();
			return all.length > 0 ? all[0] : null;
		}
	}
	
	/**
	 * Contacts the update host, and checks the latest stated version of GHSC.
	 * @param notify - whether it's ok to display a version failed message box.
	 * @return the latest online version.
	 */
	public Version refresh(final boolean notify) {
		try {
			final URL url = new URL(Paths.WEBHOST_VERSION);
			final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			synchronized (this.versionSync) {
				this.versions.clear();
				String line;
				while ((line = reader.readLine()) != null) {
					final Version v = Version.parse(line);
					if (v == null) {
                        throw new Exception("Version parse error!");
                    }
					this.versions.add(v);
				}
			}
		} catch (final Exception e) {
			if (notify) {
				JOptionPane.showMessageDialog(null, "Unable to reach version host!", "Version error", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		return this.getLatest();
	}
	
	/**
	 * Determines if the given version is a compatible version with the current running version.
	 * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
	 */
	public boolean isCompatible(final Version v) {
		return v.equals(Application.VERSION) || !this.isValid(v, v1 -> v1.hasFlag(Version.COMPATIBLE));
	}
	
	/**
	 * Determines if the given version is a compatible version with the current running version.
	 * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
	 */
	public boolean isRequired(final Version v) {
		return this.isValid(v, v1 -> v1.hasFlag(Version.REQUIRED));
	}
	
	/**
	 * Determines if the given version is a compatible version with the current running version.
	 * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
	 */
	public boolean isForced(final Version v) {
		return this.isValid(v, v1 -> v1.hasFlag(Version.FORCED));
	}
	
	/**
	 * Scans the versions for a condition.
	 * @param to The latest version.
	 * @param filter The filter to qualify versions.
	 */
    private boolean isValid(final Version to, final Filter<Version> filter) {
		synchronized (this.versionSync) {
			if (!this.versions.contains(to) || !this.versions.contains(Application.VERSION)) {
                return false;
            }
			final Version[] vall = this.getAll();
			final Version[] vm = { to, Application.VERSION };
			Arrays.sort(vm);
			final Version latest = vm[0];
			final Version current = vm[1];
			boolean checking = false;
			for (final Version c : vall) {
				if (checking) {
					if (current.equals(c)) {
                        break;
                    }
				} else {
					if (latest.equals(c)) {
                        checking = true;
                    }
				}
				if (checking) {
					if (filter.accept(c)) {
                        return true;
                    }
				}
			}
			return false;
		}
	}
	
}