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
 * Creates a controller to manage different versions of GHSC interacting with the running version.
 * @author Odell
 */
public class VersionController {
	
	private int DELAY = 10000;
	
	private Application application;
	private ArrayList<Version> versions;
	private Object versionSync = new Object();
	
	private Thread workThread;
	private Runnable workRunnable = new Runnable() {
		public void run() {
			try {
				while (running) {
					Version lastLatest = getLatest();
					Version newLatest = refresh(false);
					if (!running)
						break;
					if (lastLatest.compareTo(newLatest) > 0) {
						new Thread(new Runnable() {
							public void run() {
								application.getUpdater().updateCheck(false, false);
							}
						}).start();
					}
					Thread.sleep(DELAY);
				}
			} catch (Exception e) {}
		}
	};
	private boolean running = true;
	
	/**
	 * Initializes a new VersionController.
	 */
	public VersionController() {
		this.application = Application.getApplication();
		versions = new ArrayList<Version>();
	}
	
	/**
	 * Starts the threaded version monitor.
	 */
	public void start() {
		workThread = new Thread(workRunnable);
		workThread.setName("Version controller");
		running = true;
		workThread.start();
	}
	
	/**
	 * Stops the threaded version monitor.
	 */
	public void stop() {
		if (workThread != null && workThread.isAlive()) {
			running = false;
			workThread.interrupt();
		}
	}
	
	/**
	 * @return a sorted array of all the Versions currently loaded.
	 */
	private Version[] getAll() {
		Version[] va = versions.toArray(new Version[versions.size()]);
		Arrays.sort(va);
		return va;
	}
	
	/**
	 * @return the latest known version of GHSC.
	 */
	public Version getLatest() {
		synchronized (versionSync) {
			Version[] all = getAll();
			return all.length > 0 ? all[0] : null;
		}
	}
	
	/**
	 * Contacts the update host, and checks the latest stated version of GHSC.
	 * @param notify - whether it's ok to display a version failed message box.
	 * @return the latest online version.
	 */
	public Version refresh(boolean notify) {
		try {
			URL url = new URL(Paths.WEBHOST_VERSION);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			synchronized (versionSync) {
				versions.clear();
				String line;
				while ((line = reader.readLine()) != null) {
					Version v = Version.parse(line);
					if (v == null)
						throw new Exception("Version parse error!");
					versions.add(v);
				}
			}
		} catch (Exception e) {
			if (notify) {
				JOptionPane.showMessageDialog(null, "Unable to reach version host!", "Version error", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		return getLatest();
	}
	
	/**
	 * Determines if the given version is a compatible version with the current running version.
	 * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
	 */
	public boolean isCompatible(Version v) {
		if (v.equals(Application.VERSION))
			return true;
		return !isValid(v, new Filter<Version>() {
			public boolean accept(Version v) {
				return v.hasFlag(Version.COMPATIBLE);
			}
		});
	}
	
	/**
	 * Determines if the given version is a compatible version with the current running version.
	 * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
	 */
	public boolean isRequired(Version v) {
		return isValid(v, new Filter<Version>() {
			public boolean accept(Version v) {
				return v.hasFlag(Version.REQUIRED);
			}
		});
	}
	
	/**
	 * Determines if the given version is a compatible version with the current running version.
	 * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
	 */
	public boolean isForced(Version v) {
		return isValid(v, new Filter<Version>() {
			public boolean accept(Version v) {
				return v.hasFlag(Version.FORCED);
			}
		});
	}
	
	/**
	 * Scans the versions for a condition.
	 * @param to The latest version.
	 * @param filter The filter to qualify versions.
	 */
	protected boolean isValid(Version to, Filter<Version> filter) {
		synchronized (versionSync) {
			if (!versions.contains(to) || !versions.contains(Application.VERSION))
				return false;
			Version[] vall = getAll(), vm = { to, Application.VERSION };
			Arrays.sort(vm);
			Version latest = vm[0], current = vm[1];
			boolean checking = false;
			for (Version c : vall) {
				if (checking) {
					if (current.equals(c))
						break;
				} else {
					if (latest.equals(c))
						checking = true;
				}
				if (checking) {
					if (filter.accept(c))
						return true;
				}
			}
			return false;
		}
	}
	
}