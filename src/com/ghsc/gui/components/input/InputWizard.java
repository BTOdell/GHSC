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
	
	private Image dialogIcon = null;
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
		
		initComponents();
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
		
		initComponents();
	}
	
	/**
	 * Validates the current input text, and makes appropriate changes the visual interface.
	 */
	private void doValidate() {
		String current = InputNameField.getText();
		ValidationResult<String, Boolean> validation = validator.validate(current);
		if (validation.getResult()) {
			ValidationImage.setIcon(check);
			ValidationImage.setToolTipText(validation.getValue());
		} else {
			ValidationImage.setIcon(x);
			ValidationImage.setToolTipText(validation.getValue());
		}
	}
	
	/**
	 * Notifies the wizard listener with a 'null' value and disposes of this dialog window.
	 */
	private void close() {
		listener.wizardFinished(null);
		dispose();
	}
	
	/**
	 * Creates the visual interface of this InputWizard.
	 */
	private void initComponents() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				close();
			}
		});
		setIconImage(dialogIcon);
		setFont(Fonts.GLOBAL);
		setTitle(title);
		setAlwaysOnTop(true);
		setResizable(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setSize(273, 130);
		setLocationRelativeTo(frame);
		
		getContentPane().setLayout(null);
		getContentPane().add(getChannelNameLabel());
		getContentPane().add(getChannelNameField());
		getContentPane().add(getValidationImage());
		getContentPane().add(getJoinButton());
		getContentPane().add(getCancelButton());
		
		doValidate();
	}
	
	private JLabel getChannelNameLabel() {
		if (InputNameLabel == null) {
			InputNameLabel = new JLabel(label);
			InputNameLabel.setFont(Fonts.GLOBAL);
			InputNameLabel.setBounds(23, 11, 180, 20);
		}
		return InputNameLabel;
	}
	
	private JTextField getChannelNameField() {
		if (InputNameField == null) {
			InputNameField = new JTextField();
			InputNameField.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent arg0) {
					InputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
			});
			InputNameField.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent arg0) {
					InputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
			});
			InputNameField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					InputNameField.setBorder(UIManager.getBorder("TextField.border"));
				}
				public void keyReleased(KeyEvent e) {
					doValidate();
				}
			});
			InputNameField.setBounds(23, 31, 180, 20);
			InputNameField.setColumns(10);
			if (pretext != null && !pretext.isEmpty()) {
				InputNameField.setText(pretext);
				InputNameField.selectAll();
			}
		}
		return InputNameField;
	}
	
	private JButton getJoinButton() {
		if (JoinButton == null) {
			JoinButton = new JButton(join);
			JoinButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (validator.validate(InputNameField.getText()).getResult()) {
						listener.wizardFinished(InputNameField.getText());
						dispose();
					} else {
						InputNameField.setBorder(errorBorder);
					}
				}
			});
			JoinButton.setFont(Fonts.GLOBAL);
			JoinButton.setToolTipText(tooltip);
			JoinButton.setActionCommand("OK");
			JoinButton.setBounds(23, 62, 105, 23);
			getRootPane().setDefaultButton(JoinButton);
		}
		return JoinButton;
	}
	
	private JButton getCancelButton() {
		if (CancelButton == null) {
			CancelButton = new JButton("Cancel");
			CancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					close();
				}
			});
			CancelButton.setFont(Fonts.GLOBAL);
			CancelButton.setToolTipText("Cancels this wizard.");
			CancelButton.setActionCommand("Cancel");
			CancelButton.setBounds(138, 62, 105, 23);
		}
		return CancelButton;
	}
	
	private JLabel getValidationImage() {
		if (ValidationImage == null) {
			ValidationImage = new JLabel();
			ValidationImage.setIcon(x);
			ValidationImage.setToolTipText("Well, you actually have to type something...");
			ValidationImage.setBounds(215, 11, 48, 55);
		}
		return ValidationImage;
	}
	
}