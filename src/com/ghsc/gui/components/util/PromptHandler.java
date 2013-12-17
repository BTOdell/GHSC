package com.ghsc.gui.components.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

/**
 * Handles prompts for text components that specify what the text component is for.
 * @author Odell
 */
public class PromptHandler implements FocusListener {
	
	private final JTextComponent textComp;
	private final JLabel overlay;
	
	private boolean enabled = false;
	
	/**
	 * Creates a new prompt handler for the given text component.
	 * @param textComp
	 * 		the text component to handle.
	 * @param text
	 * 		the text to prompt over the component.
	 */
	public PromptHandler(final JTextComponent textComp, final String text) {
		this(textComp, text, Color.LIGHT_GRAY);
	}
	
	/**
	 * Creates a new prompt handler for the given text component with a prompt color.
	 * @param textComp
	 * 		the text component to handle.
	 * @param text
	 * 		the text to prompt over the component.
	 * @param color
	 * 		the color of the prompt text.
	 */
	public PromptHandler(final JTextComponent textComp, final String text, final Color color) {
		this(textComp, text, color, JLabel.CENTER);
	}
	
	public PromptHandler(final JTextComponent textComp, final String text, final Color color, final int verticalAlignment) {
		if (textComp == null)
			throw new IllegalArgumentException("Component is null!");
		this.textComp = textComp;
		
		overlay = new JLabel();
		overlay.setText(text);
		overlay.setForeground(color);
		overlay.setFont(textComp.getFont());
		overlay.setHorizontalAlignment(JLabel.LEADING);
		overlay.setVerticalAlignment(verticalAlignment);
		overlay.setVisible(false);
		
		textComp.setLayout(new BorderLayout());
		textComp.add(overlay);
		
		setEnabled(true);
		determinePrompt();
	}
	
	/**
	 * @return the text component that this PromptHandler is handling.
	 */
	public JTextComponent getComponent() {
		return textComp;
	}
	
	/**
	 * @return the prompt text currently in use.
	 */
	public String getText() {
		return overlay.getText();
	}
	
	/**
	 * Sets a new text as prompt text.
	 * @param text
	 * 		the new prompt text.
	 */
	public void setText(final String text) {
		this.overlay.setText(text);
	}
	
	/**
	 * @return the current color of the prompt text.
	 */
	public Color getColor() {
		return overlay.getForeground();
	}
	
	/**
	 * Sets a new text color for the prompt.
	 * @param color
	 * 		the new prompt text color.
	 */
	public void setColor(final Color color) {
		this.overlay.setForeground(color);
	}
	
	/**
	 * @return whether this prompt handler is active over the component.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Enables or disables prompt handling.
	 * @param enabled
	 * 		whether to enable or disable.
	 */
	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled)
			return;
		if (this.enabled = enabled) {
			textComp.addFocusListener(this);
		} else {
			setVisible(false);
			textComp.removeFocusListener(this);
		}
	}
	
	private boolean isVisible() {
		return overlay.isVisible();
	}
	
	private void setVisible(final boolean visible) {
		this.overlay.setVisible(visible);
	}
	
	private void determinePrompt() {
		if (textComp.hasFocus()) {
			if (isVisible()) {
				setVisible(false);
			}
		} else {
			if (textComp.getText().length() <= 0) {
				setVisible(true);
			}
		}
	}

	@Override
	public void focusGained(FocusEvent fe) {
		if (!textComp.equals(fe.getSource()))
			return;
		determinePrompt();
	}

	@Override
	public void focusLost(FocusEvent fe) {
		if (!textComp.equals(fe.getSource()))
			return;
		determinePrompt();
	}
	
}