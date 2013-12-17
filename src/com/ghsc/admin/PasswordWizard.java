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
	
	private SwingWorker<Boolean, String> passwordWorker = null;
	
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
		
		initComponents();
	}
	
	@Override
	public void setCursor(Cursor cursor) {
		for (Component comp : getComponents()) {
			if (comp != null)
				comp.setCursor(cursor);
		}
		super.setCursor(cursor);
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
		setIconImage(Images.KEY);
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
		getContentPane().add(getLoginButton());
		getContentPane().add(getCancelButton());
	}
	
	private JLabel getChannelNameLabel() {
		if (PasswordLabel == null) {
			PasswordLabel = new JLabel(label);
			PasswordLabel.setFont(Fonts.GLOBAL);
			PasswordLabel.setBounds(23, 11, 180, 20);
		}
		return PasswordLabel;
	}
	
	private JPasswordField getChannelNameField() {
		if (PasswordField == null) {
			PasswordField = new JPasswordField();
			PasswordField.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent arg0) {
					PasswordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			PasswordField.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent arg0) {
					PasswordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			PasswordField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					PasswordField.setBorder(UIManager.getBorder("PasswordField.border"));
				}
			});
			PasswordField.setBounds(23, 31, 220, 20);
			PasswordField.setColumns(10);
		}
		return PasswordField;
	}
	
	private JButton getLoginButton() {
		if (LoginButton == null) {
			LoginButton = new JButton("Login");
			LoginButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (passwordWorker == null) {
						final String pass = new String(PasswordField.getPassword());
						passwordWorker = new SwingWorker<Boolean, String>() {
							
							protected Boolean doInBackground() throws Exception {
								return validator.validate(pass).getResult();
							}

							protected void done() {
								try {
									boolean result = get();
									if (result) {
										listener.wizardFinished(pass);
										dispose();
									} else {
										PasswordField.setBorder(errorBorder);
									}
								} catch (Exception e) {
								} finally {
									PasswordWizard.this.setCursor(null);
									passwordWorker = null;
								}
							}

						};
						passwordWorker.execute();
						
						PasswordWizard.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
				}
			});
			LoginButton.setFont(Fonts.GLOBAL);
			LoginButton.setActionCommand("OK");
			LoginButton.setBounds(23, 62, 105, 23);
			getRootPane().setDefaultButton(LoginButton);
		}
		return LoginButton;
	}
	
	private JButton getCancelButton() {
		if (CancelButton == null) {
			CancelButton = new JButton("Cancel");
			CancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (passwordWorker != null)
						passwordWorker.cancel(true);
					close();
				}
			});
			CancelButton.setFont(Fonts.GLOBAL);
			CancelButton.setToolTipText("Cancels the login.");
			CancelButton.setActionCommand("Cancel");
			CancelButton.setBounds(138, 62, 105, 23);
		}
		return CancelButton;
	}
	
}