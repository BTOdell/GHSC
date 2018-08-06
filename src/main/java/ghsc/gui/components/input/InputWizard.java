package ghsc.gui.components.input;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import ghsc.common.Fonts;
import ghsc.common.Images;
import ghsc.gui.MainFrame;

/**
 * Provides an interactive dialog wizard, with a single text field as well as a text validator.
 */
public class InputWizard extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private final MainFrame frame;
	private final WizardListener<String> listener;
	private final WizardValidator<String, String, Boolean> validator;
	private final String title;
	private final String label;
	private final String pretext;
	private final String join;
	private final String tooltip;
	
	private final Image dialogIcon;
	private final ImageIcon check = new ImageIcon(Images.CHECK);
	private final ImageIcon x = new ImageIcon(Images.X);
	
	private final Border errorBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED), BorderFactory.createEmptyBorder(0, 2, 0, 0));
	
	private JLabel inputNameLabel;
	private JTextField inputNameField;
	private JLabel ValidationImage;
	private JButton joinButton;
	private JButton cancelButton;

	/**
	 * Create the dialog.
	 */
	public InputWizard(final MainFrame frame, final Image icon, final String title, final String label, final String pretext, final String joinText, final String joinTooltip, final WizardListener<String> listener, final WizardValidator<String, String, Boolean> validator) {
		super(frame);
		this.frame = frame;
		this.dialogIcon = icon;
		this.title = title;
		this.label = label;
		this.pretext = pretext;
		this.join = joinText;
		this.tooltip = joinTooltip;
		this.listener = listener;
		this.validator = validator;

		this.initComponents();
	}
	
	/**
	 * Create the dialog.
	 */
	public InputWizard(final MainFrame frame, final String title, final String label, final String pretext, final String joinText, final String joinTooltip, final WizardListener<String> listener, final WizardValidator<String, String, Boolean> validator) {
		super(frame);
		this.frame = frame;
		this.dialogIcon = frame != null ? frame.getIconImage() : null;
		this.title = title;
		this.label = label;
		this.pretext = pretext;
		this.join = joinText;
		this.tooltip = joinTooltip;
		this.listener = listener;
		this.validator = validator;

		this.initComponents();
	}
	
	/**
	 * Validates the current input text, and makes appropriate changes the visual interface.
	 */
	private void doValidate() {
		final String current = this.inputNameField.getText();
		final ValidationResult<String, Boolean> validation = this.validator.validate(current);
		if (validation.getResult()) {
            this.ValidationImage.setIcon(this.check);
            this.ValidationImage.setToolTipText(validation.getValue());
		} else {
            this.ValidationImage.setIcon(this.x);
            this.ValidationImage.setToolTipText(validation.getValue());
		}
	}
	
	/**
	 * Notifies the wizard listener with a 'null' value and disposes of this dialog window.
	 */
	private void close() {
        this.listener.wizardFinished(null);
		this.dispose();
	}
	
	/**
	 * Creates the visual interface of this InputWizard.
	 */
	private void initComponents() {
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent arg0) {
				InputWizard.this.close();
			}
		});
		this.setIconImage(this.dialogIcon);
		this.setFont(Fonts.GLOBAL);
		this.setTitle(this.title);
		this.setAlwaysOnTop(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.setSize(273, 130);
		this.setLocationRelativeTo(this.frame);

		this.getContentPane().setLayout(null);
		this.getContentPane().add(this.getChannelNameLabel());
		this.getContentPane().add(this.getChannelNameField());
		this.getContentPane().add(this.getValidationImage());
		this.getContentPane().add(this.getJoinButton());
		this.getContentPane().add(this.getCancelButton());

		this.doValidate();
	}
	
	private JLabel getChannelNameLabel() {
		if (this.inputNameLabel == null) {
            this.inputNameLabel = new JLabel(this.label);
            this.inputNameLabel.setFont(Fonts.GLOBAL);
            this.inputNameLabel.setBounds(23, 11, 180, 20);
		}
		return this.inputNameLabel;
	}
	
	private JTextField getChannelNameField() {
		if (this.inputNameField == null) {
            this.inputNameField = new JTextField();
            this.inputNameField.addFocusListener(new FocusAdapter() {
				public void focusGained(final FocusEvent arg0) {
                    InputWizard.this.inputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
			});
            this.inputNameField.addMouseListener(new MouseAdapter() {
				public void mousePressed(final MouseEvent arg0) {
                    InputWizard.this.inputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
			});
            this.inputNameField.addKeyListener(new KeyAdapter() {
				public void keyPressed(final KeyEvent e) {
                    InputWizard.this.inputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
				public void keyReleased(final KeyEvent e) {
					InputWizard.this.doValidate();
				}
			});
            this.inputNameField.setBounds(23, 31, 180, 20);
            this.inputNameField.setColumns(10);
			if (this.pretext != null && !this.pretext.isEmpty()) {
                this.inputNameField.setText(this.pretext);
                this.inputNameField.selectAll();
			}
		}
		return this.inputNameField;
	}
	
	private JButton getJoinButton() {
		if (this.joinButton == null) {
			this.joinButton = new JButton(this.join);
			this.joinButton.addActionListener(event -> {
				if (this.validator.validate(this.inputNameField.getText()).getResult()) {
					this.listener.wizardFinished(this.inputNameField.getText());
					this.dispose();
				} else {
					this.inputNameField.setBorder(this.errorBorder);
				}
			});
            this.joinButton.setFont(Fonts.GLOBAL);
            this.joinButton.setToolTipText(this.tooltip);
            this.joinButton.setActionCommand("OK");
            this.joinButton.setBounds(23, 62, 105, 23);
			this.getRootPane().setDefaultButton(this.joinButton);
		}
		return this.joinButton;
	}
	
	private JButton getCancelButton() {
		if (this.cancelButton == null) {
            this.cancelButton = new JButton("Cancel");
            this.cancelButton.addActionListener(event -> this.close());
            this.cancelButton.setFont(Fonts.GLOBAL);
            this.cancelButton.setToolTipText("Cancels this wizard.");
            this.cancelButton.setActionCommand("Cancel");
            this.cancelButton.setBounds(138, 62, 105, 23);
		}
		return this.cancelButton;
	}
	
	private JLabel getValidationImage() {
		if (this.ValidationImage == null) {
            this.ValidationImage = new JLabel();
            this.ValidationImage.setIcon(this.x);
            this.ValidationImage.setToolTipText("Well, you actually have to type something...");
            this.ValidationImage.setBounds(215, 11, 48, 55);
		}
		return this.ValidationImage;
	}
	
}