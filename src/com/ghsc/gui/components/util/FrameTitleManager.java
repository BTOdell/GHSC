package com.ghsc.gui.components.util;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

/**
 * Dynamically manage the title of frames.<br>
 * Temporary titles with timeouts.
 * @author Odell
 */
public abstract class FrameTitleManager {
	
	private final Frame frame;
	private String defaultTitle;
	
	private Timer timer = null;
	private ActionListener event = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			appendTitle(null);
		}
	};
	
	/**
	 * Initializes a new FrameTitleManager.
	 * @param frame
	 * 		the frame to control.
	 * @param defaultTitle
	 * 		the default title of this frame.
	 */
	public FrameTitleManager(final Frame frame, final String defaultTitle) {
		this.frame = frame;
		setDefaultTitle(defaultTitle);
		timer = new Timer(0, event);
		timer.setRepeats(false);
	}
	
	public final String getDefaultTitle() {
		return defaultTitle;
	}
	
	/**
	 * Changes the default title of this frame.</br>
	 * @param defaultTitle
	 * 		the new default title of the controlled frame.
	 */
	public final void setDefaultTitle(final String defaultTitle) {
		setDefaultTitle(defaultTitle, false);
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
		if (modify && (timer == null || !timer.isRunning()))
			reset();
	}
	
	private void stopTimer() {
		if (timer.isRunning())
			timer.stop();
	}
	
	private void restartTimer(int delay) {
		stopTimer();
		timer.setInitialDelay(delay);
		timer.restart();
	}
	
	/**
	 * Sets the title of the controlled frame to the default title.
	 */
	public final void reset() {
		setTitle(defaultTitle);
	}
	
	/**
	 * Sets the title of the controlled frame.</br>
	 * @param title
	 * 		the suggested title of the frame.
	 */
	public final void setTitle(final String title) {
		if (frame != null)
			frame.setTitle(title);
		onTitleChanged(title);
	}
	
	/**
	 * Modifies given text to the default title of the controlled frame.</b>
	 * @param append 
	 * 		the text to append the default title.
	 */
	public final void appendTitle(final String append) {
		final StringBuilder build = new StringBuilder().append(defaultTitle);
		setTitle((append != null ? build.append(' ').append(append) : build).toString());
	}
	
	/**
	 * Submits a temporary title to the frame.
	 * @param title
	 * 		the temporary title.
	 * @param period
	 * 		the time period in milliseconds for the temporary title to last.
	 */
	public final void submit(final String title, final int period) {
		setTitle(title);
		stopTimer();
		if (period > 0)
			restartTimer(period);
	}
	
	/**
	 * Submits temporary text to the title of the frame.
	 * @param append
	 * 		the temporary append text.
	 * @param period
	 * 		the time period in milliseconds for the temporary text to last.
	 */
	public final void submitAppend(final String append, final int period) {
		appendTitle(append);
		stopTimer();
		if (period > 0)
			restartTimer(period);
	}
	
	/**
	 * Occurs when the title of this frame changes due to this FrameTitleManager.<br>
	 * This will not listen for changes to the title done manually.
	 * @param title
	 * 		the new title of the controlled frame.
	 */
	public abstract void onTitleChanged(final String title);
	
}