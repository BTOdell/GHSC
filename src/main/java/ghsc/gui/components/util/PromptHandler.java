package ghsc.gui.components.util;

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
	
	private boolean enabled;
	
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
		if (textComp == null) {
            throw new IllegalArgumentException("Component is null!");
        }
		this.textComp = textComp;

		this.overlay = new JLabel();
		this.overlay.setText(text);
		this.overlay.setForeground(color);
		this.overlay.setFont(textComp.getFont());
		this.overlay.setHorizontalAlignment(JLabel.LEADING);
		this.overlay.setVerticalAlignment(verticalAlignment);
		this.overlay.setVisible(false);
		
		textComp.setLayout(new BorderLayout());
		textComp.add(this.overlay);

		this.setEnabled(true);
		this.determinePrompt();
	}
	
	/**
	 * @return the text component that this PromptHandler is handling.
	 */
	public JTextComponent getComponent() {
		return this.textComp;
	}
	
	/**
	 * @return the prompt text currently in use.
	 */
	public String getText() {
		return this.overlay.getText();
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
		return this.overlay.getForeground();
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
		return this.enabled;
	}
	
	/**
	 * Enables or disables prompt handling.
	 * @param enabled
	 * 		whether to enable or disable.
	 */
	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) {
            return;
        }
		if (this.enabled = enabled) {
			this.textComp.addFocusListener(this);
		} else {
			this.setVisible(false);
			this.textComp.removeFocusListener(this);
		}
	}
	
	private boolean isVisible() {
		return this.overlay.isVisible();
	}
	
	private void setVisible(final boolean visible) {
		this.overlay.setVisible(visible);
	}
	
	private void determinePrompt() {
		if (this.textComp.hasFocus()) {
			if (this.isVisible()) {
				this.setVisible(false);
			}
		} else {
			if (this.textComp.getText().length() <= 0) {
				this.setVisible(true);
			}
		}
	}

	@Override
	public void focusGained(final FocusEvent fe) {
		if (!this.textComp.equals(fe.getSource())) {
            return;
        }
		this.determinePrompt();
	}

	@Override
	public void focusLost(final FocusEvent fe) {
		if (!this.textComp.equals(fe.getSource())) {
            return;
        }
		this.determinePrompt();
	}
	
}