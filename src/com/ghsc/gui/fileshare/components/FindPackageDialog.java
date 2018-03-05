package com.ghsc.gui.fileshare.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.components.input.WizardListener;
import com.ghsc.gui.components.input.WizardValidator;
import com.ghsc.gui.fileshare.FileShareFrame;
import com.ghsc.gui.fileshare.internal.RemotePackage;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class FindPackageDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private FileShareFrame frame;
	private WizardListener<RemotePackage[]> listener;
	private WizardValidator<String, RemotePackage[], Boolean> validator;
	
	private Border errorBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED), BorderFactory.createEmptyBorder(0, 2, 0, 0));
	
	private SwingWorker<ValidationResult<RemotePackage[], Boolean>, String> discoverWorker;
	
	private JLabel keyLabel;
	private JTextField keyField;
	private JButton okButton;
	private JButton cancelButton;
	
	/**
	 * Create the dialog.
	 */
	public FindPackageDialog(FileShareFrame frame, WizardListener<RemotePackage[]> listener, WizardValidator<String, RemotePackage[], Boolean> validator) {
		super(frame);
		this.frame = frame;
		this.listener = listener;
		this.validator = validator;

		this.initComponents();
	}
	
	private void close() {
		this.listener.wizardFinished(null);
		this.dispose();
	}
	
	private void initComponents() {
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				FindPackageDialog.this.close();
			}
		});
		this.setIconImage(Images.FIND);
		this.setFont(Fonts.GLOBAL);
		this.setTitle("Find private package");
		this.setAlwaysOnTop(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.setSize(273, 130);
		this.setLocationRelativeTo(this.frame);

		this.getContentPane().setLayout(null);
		this.getContentPane().add(this.getKeyLabel());
		this.getContentPane().add(this.getKeyField());
		this.getContentPane().add(this.getOkButton());
		this.getContentPane().add(this.getCancelButton());
	}
	
	private JLabel getKeyLabel() {
		if (this.keyLabel == null) {
			this.keyLabel = new JLabel("Enter a private package key.");
			this.keyLabel.setFont(Fonts.GLOBAL);
			this.keyLabel.setBounds(23, 11, 220, 20);
		}
		return this.keyLabel;
	}
	
	private JTextField getKeyField() {
		if (this.keyField == null) {
			this.keyField = new JTextField();
			this.keyField.setBounds(23, 31, 220, 20);
			this.keyField.setColumns(10);
			// TODO: make sure the user can only enter in private keys to the specified format
		}
		return this.keyField;
	}
	
	private JButton getOkButton() {
		if (this.okButton == null) {
			this.okButton = new JButton("OK");
			this.okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (FindPackageDialog.this.discoverWorker == null) {
						final String key = FindPackageDialog.this.getKeyField().getText();
						FindPackageDialog.this.discoverWorker = new SwingWorker<ValidationResult<RemotePackage[], Boolean>, String>() {
							protected ValidationResult<RemotePackage[], Boolean> doInBackground() throws Exception {
								return FindPackageDialog.this.validator.validate(key);
							}
							protected void done() {
								try {
									ValidationResult<RemotePackage[], Boolean> result = this.get();
									if (result != null && result.getResult()) {
										FindPackageDialog.this.listener.wizardFinished(result.getValue());
										FindPackageDialog.this.dispose();
									} else {
										FindPackageDialog.this.getKeyField().setBorder(FindPackageDialog.this.errorBorder);
									}
								} catch (Exception ignored) {
								} finally {
									FindPackageDialog.this.discoverWorker = null;
								}
							}
						};
						FindPackageDialog.this.discoverWorker.execute();
					}
				}
			});
			this.okButton.setFont(Fonts.GLOBAL);
			this.okButton.setActionCommand("OK");
			this.okButton.setBounds(23, 62, 105, 23);
			this.getRootPane().setDefaultButton(this.okButton);
		}
		return this.okButton;
	}
	
	private JButton getCancelButton() {
		if (this.cancelButton == null) {
			this.cancelButton = new JButton("Cancel");
			this.cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (FindPackageDialog.this.discoverWorker != null) {
						FindPackageDialog.this.discoverWorker.cancel(true);
                    }
					FindPackageDialog.this.close();
				}
			});
			this.cancelButton.setFont(Fonts.GLOBAL);
			this.cancelButton.setToolTipText("Cancels this dialog.");
			this.cancelButton.setActionCommand("Cancel");
			this.cancelButton.setBounds(138, 62, 105, 23);
		}
		return this.cancelButton;
	}

}