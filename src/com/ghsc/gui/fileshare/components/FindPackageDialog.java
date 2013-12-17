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
	
	private SwingWorker<ValidationResult<RemotePackage[], Boolean>, String> discoverWorker = null;
	
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
		
		initComponents();
	}
	
	private void close() {
		listener.wizardFinished(null);
		dispose();
	}
	
	private void initComponents() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				close();
			}
		});
		setIconImage(Images.FIND);
		setFont(Fonts.GLOBAL);
		setTitle("Find private package");
		setAlwaysOnTop(true);
		setResizable(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setSize(273, 130);
		setLocationRelativeTo(frame);
	
		getContentPane().setLayout(null);
		getContentPane().add(getKeyLabel());
		getContentPane().add(getKeyField());
		getContentPane().add(getOkButton());
		getContentPane().add(getCancelButton());
	}
	
	private JLabel getKeyLabel() {
		if (keyLabel == null) {
			keyLabel = new JLabel("Enter a private package key.");
			keyLabel.setFont(Fonts.GLOBAL);
			keyLabel.setBounds(23, 11, 220, 20);
		}
		return keyLabel;
	}
	
	private JTextField getKeyField() {
		if (keyField == null) {
			keyField = new JTextField();
			keyField.setBounds(23, 31, 220, 20);
			keyField.setColumns(10);
			// TODO: make sure the user can only enter in private keys to the specified format
		}
		return keyField;
	}
	
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (discoverWorker == null) {
						final String key = getKeyField().getText();
						discoverWorker = new SwingWorker<ValidationResult<RemotePackage[], Boolean>, String>() {
							protected ValidationResult<RemotePackage[], Boolean> doInBackground() throws Exception {
								return validator.validate(key);
							}
							protected void done() {
								try {
									ValidationResult<RemotePackage[], Boolean> result = get();
									if (result != null && result.getResult()) {
										listener.wizardFinished(result.getValue());
										dispose();
									} else {
										getKeyField().setBorder(errorBorder);
									}
								} catch (Exception e) {
								} finally {
									discoverWorker = null;
								}
							}
						};
						discoverWorker.execute();
					}
				}
			});
			okButton.setFont(Fonts.GLOBAL);
			okButton.setActionCommand("OK");
			okButton.setBounds(23, 62, 105, 23);
			getRootPane().setDefaultButton(okButton);
		}
		return okButton;
	}
	
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (discoverWorker != null)
						discoverWorker.cancel(true);
					close();
				}
			});
			cancelButton.setFont(Fonts.GLOBAL);
			cancelButton.setToolTipText("Cancels this dialog.");
			cancelButton.setActionCommand("Cancel");
			cancelButton.setBounds(138, 62, 105, 23);
		}
		return cancelButton;
	}

}