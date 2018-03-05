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
	
	private MainFrame frame;
	private WizardListener<String> listener;
	private WizardValidator<String, String, Boolean> validator;
	private String title, label;
	
	private Border errorBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED), BorderFactory.createEmptyBorder(0, 2, 0, 0));
	
	private SwingWorker<Boolean, String> passwordWorker;
	
	private JLabel PasswordLabel;
	private JPasswordField PasswordField;
	private JButton LoginButton;
	private JButton CancelButton;

	/**
	 * Create the dialog.
	 */
	public PasswordWizard(MainFrame frame, String title, String label, WizardListener<String> listener, WizardValidator<String, String, Boolean> validator) {
		super(frame);
		this.frame = frame;
		this.title = title;
		this.label = label;
		this.listener = listener;
		this.validator = validator;

		this.initComponents();
	}
	
	@Override
	public void setCursor(Cursor cursor) {
		for (Component comp : this.getComponents()) {
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
			public void windowClosing(WindowEvent arg0) {
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
		if (this.PasswordLabel == null) {
			this.PasswordLabel = new JLabel(this.label);
			this.PasswordLabel.setFont(Fonts.GLOBAL);
			this.PasswordLabel.setBounds(23, 11, 180, 20);
		}
		return this.PasswordLabel;
	}
	
	private JPasswordField getChannelNameField() {
		if (this.PasswordField == null) {
			this.PasswordField = new JPasswordField();
			this.PasswordField.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent arg0) {
					PasswordWizard.this.PasswordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			this.PasswordField.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent arg0) {
					PasswordWizard.this.PasswordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			this.PasswordField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					PasswordWizard.this.PasswordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			this.PasswordField.setBounds(23, 31, 220, 20);
			this.PasswordField.setColumns(10);
		}
		return this.PasswordField;
	}
	
	private JButton getLoginButton() {
		if (this.LoginButton == null) {
			this.LoginButton = new JButton("Login");
			this.LoginButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (PasswordWizard.this.passwordWorker == null) {
						final String pass = new String(PasswordWizard.this.PasswordField.getPassword());
						PasswordWizard.this.passwordWorker = new SwingWorker<Boolean, String>() {
							
							protected Boolean doInBackground() throws Exception {
								return PasswordWizard.this.validator.validate(pass).getResult();
							}

							protected void done() {
								try {
									boolean result = this.get();
									if (result) {
										PasswordWizard.this.listener.wizardFinished(pass);
										PasswordWizard.this.dispose();
									} else {
										PasswordWizard.this.PasswordField.setBorder(PasswordWizard.this.errorBorder);
									}
								} catch (Exception ignored) {
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
			this.LoginButton.setFont(Fonts.GLOBAL);
			this.LoginButton.setActionCommand("OK");
			this.LoginButton.setBounds(23, 62, 105, 23);
			this.getRootPane().setDefaultButton(this.LoginButton);
		}
		return this.LoginButton;
	}
	
	private JButton getCancelButton() {
		if (this.CancelButton == null) {
			this.CancelButton = new JButton("Cancel");
			this.CancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (PasswordWizard.this.passwordWorker != null) {
						PasswordWizard.this.passwordWorker.cancel(true);
                    }
					PasswordWizard.this.close();
				}
			});
			this.CancelButton.setFont(Fonts.GLOBAL);
			this.CancelButton.setToolTipText("Cancels the login.");
			this.CancelButton.setActionCommand("Cancel");
			this.CancelButton.setBounds(138, 62, 105, 23);
		}
		return this.CancelButton;
	}
	
}