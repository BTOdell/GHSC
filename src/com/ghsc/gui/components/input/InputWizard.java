package com.ghsc.gui.components.input;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.gui.MainFrame;

/**
 * Provides an interactive dialog wizard, with a single text field as well as a text validator.
 * @author Odell
 */
public class InputWizard extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private MainFrame frame;
	private WizardListener<String> listener;
	private WizardValidator<String, String, Boolean> validator;
	private String title, label, pretext, join, tooltip;
	
	private Image dialogIcon;
	private ImageIcon check = new ImageIcon(Images.CHECK);
	private ImageIcon x = new ImageIcon(Images.X);
	
	private Border errorBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED), BorderFactory.createEmptyBorder(0, 2, 0, 0));
	
	private JLabel InputNameLabel;
	private JTextField InputNameField;
	private JLabel ValidationImage;
	private JButton JoinButton;
	private JButton CancelButton;

	/**
	 * Create the dialog.
	 */
	public InputWizard(MainFrame frame, Image icon, String title, String label, String pretext, String joinText, String joinTooltip, WizardListener<String> listener, WizardValidator<String, String, Boolean> validator) {
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
	public InputWizard(MainFrame frame, String title, String label, String pretext, String joinText, String joinTooltip, WizardListener<String> listener, WizardValidator<String, String, Boolean> validator) {
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
		String current = this.InputNameField.getText();
		ValidationResult<String, Boolean> validation = this.validator.validate(current);
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
			public void windowClosing(WindowEvent arg0) {
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
		if (this.InputNameLabel == null) {
            this.InputNameLabel = new JLabel(this.label);
            this.InputNameLabel.setFont(Fonts.GLOBAL);
            this.InputNameLabel.setBounds(23, 11, 180, 20);
		}
		return this.InputNameLabel;
	}
	
	private JTextField getChannelNameField() {
		if (this.InputNameField == null) {
            this.InputNameField = new JTextField();
            this.InputNameField.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent arg0) {
                    InputWizard.this.InputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
			});
            this.InputNameField.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent arg0) {
                    InputWizard.this.InputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
			});
            this.InputNameField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
                    InputWizard.this.InputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
				public void keyReleased(KeyEvent e) {
					InputWizard.this.doValidate();
				}
			});
            this.InputNameField.setBounds(23, 31, 180, 20);
            this.InputNameField.setColumns(10);
			if (this.pretext != null && !this.pretext.isEmpty()) {
                this.InputNameField.setText(this.pretext);
                this.InputNameField.selectAll();
			}
		}
		return this.InputNameField;
	}
	
	private JButton getJoinButton() {
		if (this.JoinButton == null) {
            this.JoinButton = new JButton(this.join);
            this.JoinButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (InputWizard.this.validator.validate(InputWizard.this.InputNameField.getText()).getResult()) {
                        InputWizard.this.listener.wizardFinished(InputWizard.this.InputNameField.getText());
						InputWizard.this.dispose();
					} else {
                        InputWizard.this.InputNameField.setBorder(InputWizard.this.errorBorder);
					}
				}
			});
            this.JoinButton.setFont(Fonts.GLOBAL);
            this.JoinButton.setToolTipText(this.tooltip);
            this.JoinButton.setActionCommand("OK");
            this.JoinButton.setBounds(23, 62, 105, 23);
			this.getRootPane().setDefaultButton(this.JoinButton);
		}
		return this.JoinButton;
	}
	
	private JButton getCancelButton() {
		if (this.CancelButton == null) {
            this.CancelButton = new JButton("Cancel");
            this.CancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					InputWizard.this.close();
				}
			});
            this.CancelButton.setFont(Fonts.GLOBAL);
            this.CancelButton.setToolTipText("Cancels this wizard.");
            this.CancelButton.setActionCommand("Cancel");
            this.CancelButton.setBounds(138, 62, 105, 23);
		}
		return this.CancelButton;
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