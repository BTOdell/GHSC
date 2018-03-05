package com.ghsc.gui.components.util;

import java.awt.Frame;

import javax.swing.Timer;

/**
 * Dynamically manage the title of frames.<br>
 * Temporary titles with timeouts.
 */
public abstract class FrameTitleManager {
	
	private final Frame frame;
	private String defaultTitle;
	
	private final Timer timer;

	/**
	 * Initializes a new FrameTitleManager.
	 * @param frame
	 * 		the frame to control.
	 * @param defaultTitle
	 * 		the default title of this frame.
	 */
	public FrameTitleManager(final Frame frame, final String defaultTitle) {
		this.frame = frame;
        this.setDefaultTitle(defaultTitle);
		this.timer = new Timer(0, e -> this.appendTitle(null));
		this.timer.setRepeats(false);
	}
	
	public final String getDefaultTitle() {
		return this.defaultTitle;
	}
	
	/**
	 * Changes the default title of this frame.</br>
	 * @param defaultTitle
	 * 		the new default title of the controlled frame.
	 */
	public final void setDefaultTitle(final String defaultTitle) {
		this.setDefaultTitle(defaultTitle, false);
	}
	
	/**
	 * Changes the default title of this frame.</br>
	 * This will automatically update the frame itself,</br>
	 * if the modify boolean is true or as long as a temporary title hasn't been submitted.
	 * @param defaultTitle
	 * 		the new default title of the controlled frame.
	 */
	public final void setDefaultTitle(final String defaultTitle, final boolean modify) {
		this.defaultTitle = defaultTitle;
		if (modify && (this.timer == null || !this.timer.isRunning())) {
			this.reset();
		}
	}
	
	private void stopTimer() {
		if (this.timer.isRunning()) {
			this.timer.stop();
		}
	}
	
	private void restartTimer(final int delay) {
        this.stopTimer();
		this.timer.setInitialDelay(delay);
		this.timer.restart();
	}
	
	/**
	 * Sets the title of the controlled frame to the default title.
	 */
	public final void reset() {
		this.setTitle(this.defaultTitle);
	}
	
	/**
	 * Sets the title of the controlled frame.</br>
	 * @param title
	 * 		the suggested title of the frame.
	 */
	public final void setTitle(final String title) {
		if (this.frame != null) {
			this.frame.setTitle(title);
		}
        this.onTitleChanged(title);
	}
	
	/**
	 * Modifies given text to the default title of the controlled frame.</b>
	 * @param append 
	 * 		the text to append the default title.
	 */
	public final void appendTitle(final String append) {
		final StringBuilder build = new StringBuilder().append(this.defaultTitle);
		this.setTitle((append != null ? build.append(' ').append(append) : build).toString());
	}
	
	/**
	 * Submits a temporary title to the frame.
	 * @param title
	 * 		the temporary title.
	 * @param period
	 * 		the time period in milliseconds for the temporary title to last.
	 */
	public final void submit(final String title, final int period) {
        this.setTitle(title);
        this.stopTimer();
		if (period > 0) {
            this.restartTimer(period);
		}
	}
	
	/**
	 * Submits temporary text to the title of the frame.
	 * @param append
	 * 		the temporary append text.
	 * @param period
	 * 		the time period in milliseconds for the temporary text to last.
	 */
	public final void submitAppend(final String append, final int period) {
        this.appendTitle(append);
        this.stopTimer();
		if (period > 0) {
            this.restartTimer(period);
		}
	}
	
	/**
	 * Occurs when the title of this frame changes due to this FrameTitleManager.<br>
	 * This will not listen for changes to the title done manually.
	 * @param title
	 * 		the new title of the controlled frame.
	 */
	public abstract void onTitleChanged(final String title);
	
}