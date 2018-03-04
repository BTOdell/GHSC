package com.ghsc.admin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.ghsc.admin.commands.AdminCommand;
import com.ghsc.admin.commands.AdminCommands;
import com.ghsc.common.Paths;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.Application;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.components.input.WizardListener;
import com.ghsc.gui.components.input.WizardValidator;
import com.ghsc.gui.components.users.User;
import com.ghsc.net.encryption.SHA2;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

public class AdminControl {
	
	private MainFrame frame;
	private PasswordWizard pw;
	private AdminCommands commands;
	
	private byte[] hash = null;
	private boolean isAdmin = false;
	
	public AdminControl() throws Exception {
		final Application application = Application.getInstance();
		this.frame = application.getMainFrame();
		this.commands = new AdminCommands(this);
		
		//this.hash = retrieveHash();
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}
	
	public void setAdmin(final boolean admin) {
		this.isAdmin = admin;
	}
	
	public Tag process(final User user, final MessageEvent adminEvent) {
		if (adminEvent.getType() != Type.ADMIN)
			return null;
		final String requestCommand = adminEvent.getAttribute(AdminCommand.ATT_COMMAND);
		if (isAdmin()) {
			if (!Utilities.resolveToBoolean(adminEvent.getAttribute(AdminCommand.ATT_UPDATE)) && 
					!Utilities.resolveToBoolean(adminEvent.getAttribute(AdminCommand.ATT_RESPONSE))) {
				return AdminCommand.composeResponse(requestCommand, true, false, "Operation not allowed: This user is also an admin!");
			}
		}
		final AdminCommand aCommand = commands.get(requestCommand);
		if (aCommand == null)
			return AdminCommand.composeResponse(requestCommand, false, false, "Command not supported: " + requestCommand);
		final String supported = adminEvent.getAttribute(AdminCommand.ATT_SUPPORTED);
		if (supported != null && !Utilities.resolveToBoolean(supported)) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					final Application application = Application.getInstance();
					JOptionPane.showMessageDialog(application.getMainFrame(), aCommand.getName() + ": not supported for " + user.getPreferredName() + "!", "Command error", JOptionPane.ERROR_MESSAGE);
				}
			});
			return null;
		}
		return aCommand.execute(user, adminEvent);
	}
	
	public boolean showLogin() {
		if (isLoginVisible())
			return false;
		pw = new PasswordWizard(frame, "Administrative login", "Password", 
		new WizardListener<String>() {
			public void wizardFinished(String input) {
				if (input != null) {
					setAdmin(true);
					frame.getAdminButton().setToolTipText("Logout");
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(frame, "Login successful: You have been confirmed as administrator.", "Administrative login", JOptionPane.INFORMATION_MESSAGE);
						}
					});
				} else {
					frame.getAdminButton().setSelected(false);
				}
			}
		}, new WizardValidator<String, String, Boolean>() {
			public ValidationResult<String, Boolean> validate(String password) {
				try {
					return new ValidationResult<String, Boolean>(null, AdminControl.this.validate(password));
				} catch (InvalidHashException e) {
					return new ValidationResult<String, Boolean>(e.getMessage(), false);
				}
			}
		});
		pw.setVisible(true);
		return true;
	}
	
	public boolean isLoginVisible() {
		return pw != null && pw.isVisible();
	}
	
	/**
	 * @return whether this admin control is ready. (meaning the admin control knows the password hash to validate)
	 */
	public boolean isReady() {
		return hash != null;
	}
	
	/*
	 * Process all password stuff here
	 */
	
	private byte[] retrieveHash() {
		InputStream io = null;
		try {
			URL url = new URL(Paths.WEBHOST_PASS);
			io = url.openStream();
			
			byte[] hash = new byte[64];
			int read, off = 0;
			while ((read = io.read(hash, off, hash.length - off)) > 0)
				off += read;
			return hash;
		} catch (UnknownHostException uhe) {
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (io != null) {
				try {
					io.close();
				} catch (IOException e) {}
			}
		}
		return null;
	}
	
	protected String refreshPassword() {
		byte[] newHash = retrieveHash();
		if (newHash == null)
			return null;
		hash = newHash;
		return new String(hash, Application.CHARSET);
	}
	
	public boolean validate(String password) throws InvalidHashException {
		frame.getStatusLabel().submit("Confirming password.", 0);
		refreshPassword();
		frame.getStatusLabel().setDefaultStatus();
		return confirm(hash, password);
	}
	
	protected static boolean confirm(byte[] hash, String input) throws InvalidHashException {
		if (hash == null)
			throw new InvalidHashException("The password hash is null, it wasn't retrieved.");
		return SHA2.verify(hash, SHA2.hash512Bytes(input));
	}
	
	public static class InvalidHashException extends Exception {
		
		private static final long serialVersionUID = 1L;
		
		public InvalidHashException(final String msg) {
			super(msg);
		}
		
	}
	
}