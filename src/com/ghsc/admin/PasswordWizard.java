package com.ghsc.admin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.input.WizardListener;
import com.ghsc.gui.components.input.WizardValidator;

/**
 * Provides an interactive password dialog, with a single password field.
 * @author Odell
 */
public class PasswordWizard extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private final MainFrame frame;
	private final WizardListener<String> listener;
	private final WizardValidator<String, String, Boolean> validator;
	private final String title;
	private final String label;
	
	private final Border errorBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED), BorderFactory.createEmptyBorder(0, 2, 0, 0));
	
	private SwingWorker<Boolean, String> passwordWorker;
	
	private JLabel passwordLabel;
	private JPasswordField passwordField;
	private JButton loginButton;
	private JButton cancelButton;

	/**
	 * Create the dialog.
	 */
	public PasswordWizard(final MainFrame frame, final String title, final String label, final WizardListener<String> listener, final WizardValidator<String, String, Boolean> validator) {
		super(frame);
		this.frame = frame;
		this.title = title;
		this.label = label;
		this.listener = listener;
		this.validator = validator;

		this.initComponents();
	}
	
	@Override
	public void setCursor(final Cursor cursor) {
		for (final Component comp : this.getComponents()) {
			if (comp != null) {
                comp.setCursor(cursor);
            }
		}
		super.setCursor(cursor);
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
				PasswordWizard.this.close();
			}
		});
		this.setIconImage(Images.KEY);
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
		this.getContentPane().add(this.getLoginButton());
		this.getContentPane().add(this.getCancelButton());
	}
	
	private JLabel getChannelNameLabel() {
		if (this.passwordLabel == null) {
			this.passwordLabel = new JLabel(this.label);
			this.passwordLabel.setFont(Fonts.GLOBAL);
			this.passwordLabel.setBounds(23, 11, 180, 20);
		}
		return this.passwordLabel;
	}
	
	private JPasswordField getChannelNameField() {
		if (this.passwordField == null) {
			this.passwordField = new JPasswordField();
			this.passwordField.addFocusListener(new FocusAdapter() {
				public void focusGained(final FocusEvent arg0) {
					PasswordWizard.this.passwordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			this.passwordField.addMouseListener(new MouseAdapter() {
				public void mousePressed(final MouseEvent arg0) {
					PasswordWizard.this.passwordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			this.passwordField.addKeyListener(new KeyAdapter() {
				public void keyPressed(final KeyEvent e) {
					PasswordWizard.this.passwordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			this.passwordField.setBounds(23, 31, 220, 20);
			this.passwordField.setColumns(10);
		}
		return this.passwordField;
	}
	
	private JButton getLoginButton() {
		if (this.loginButton == null) {
			this.loginButton = new JButton("Login");
			this.loginButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent event) {
					if (PasswordWizard.this.passwordWorker == null) {
						final String pass = new String(PasswordWizard.this.passwordField.getPassword());
						PasswordWizard.this.passwordWorker = new SwingWorker<Boolean, String>() {
							protected Boolean doInBackground() {
								return PasswordWizard.this.validator.validate(pass).getResult();
							}
							protected void done() {
								try {
									final boolean result = this.get();
									if (result) {
										PasswordWizard.this.listener.wizardFinished(pass);
										PasswordWizard.this.dispose();
									} else {
										PasswordWizard.this.passwordField.setBorder(PasswordWizard.this.errorBorder);
									}
								} catch (final Exception ignored) {
								} finally {
									PasswordWizard.this.setCursor(null);
									PasswordWizard.this.passwordWorker = null;
								}
							}
						};
						PasswordWizard.this.passwordWorker.execute();
						PasswordWizard.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
				}
			});
			this.loginButton.setFont(Fonts.GLOBAL);
			this.loginButton.setActionCommand("OK");
			this.loginButton.setBounds(23, 62, 105, 23);
			this.getRootPane().setDefaultButton(this.loginButton);
		}
		return this.loginButton;
	}
	
	private JButton getCancelButton() {
		if (this.cancelButton == null) {
			this.cancelButton = new JButton("Cancel");
			this.cancelButton.addActionListener(event -> {
                if (this.passwordWorker != null) {
					this.passwordWorker.cancel(true);
}
				this.close();
            });
			this.cancelButton.setFont(Fonts.GLOBAL);
			this.cancelButton.setToolTipText("Cancels the login.");
			this.cancelButton.setActionCommand("Cancel");
			this.cancelButton.setBounds(138, 62, 105, 23);
		}
		return this.cancelButton;
	}
	
}