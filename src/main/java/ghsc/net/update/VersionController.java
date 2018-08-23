package ghsc.net.update;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;

/**
 * Creates a controller to manage different versions of the application interacting with the running version.
 */
public class VersionController {

	private static final int DELAY = 10000;

	@Nullable
	private Release release;
    private final Object releaseLock = new Object();

	private Thread workThread;
	private final Runnable workRunnable = () -> {
        try {
            while (this.running) {
                try {
                    final Release newLatest = Release.getLatest();
                    final Release lastLatest;
                    synchronized (this.releaseLock) {
                        lastLatest = this.release;
                        this.release = newLatest;
                    }
                    if (!this.running) {
                        break;
                    }
                    if (lastLatest == null || lastLatest.compareTo(newLatest) > 0) {
                        new Thread(() -> Updater.updateCheck(false)).start();
                    }
                } catch (final IOException ignored) {}
                Thread.sleep(DELAY);
            }
        } catch (final InterruptedException ignored) {}
    };
	private boolean running = true;

	private VersionController() {}

	@Nullable
	public Release getKnownLatest() {
	    synchronized (this.releaseLock) {
	        return this.release;
        }
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

	public static VersionController createFromLatest() {
	    final VersionController controller = new VersionController();
        try {
            controller.release = Release.getLatest();
        } catch (final IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Unable to reach version host!", "Version error", JOptionPane.ERROR_MESSAGE);
        }
	    return controller;
    }
	
}