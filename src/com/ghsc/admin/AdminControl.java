package com.ghsc.admin;

import com.ghsc.admin.commands.AdminCommand;
import com.ghsc.admin.commands.AdminCommands;
import com.ghsc.common.Paths;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.Application;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.components.users.User;
import com.ghsc.net.encryption.SHA2;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;

public class AdminControl {
	
	private final MainFrame frame;
	private final AdminCommands commands;
	private PasswordWizard pw;

	private byte[] hash;
	private boolean isAdmin;
	
	public AdminControl() {
		final Application application = Application.getInstance();
		this.frame = application.getMainFrame();
		this.commands = new AdminCommands(this);
		
		//this.hash = retrieveHash();
	}
	
	public boolean isAdmin() {
		return this.isAdmin;
	}
	
	public void setAdmin(final boolean admin) {
		this.isAdmin = admin;
	}
	
	public Tag process(final User user, final MessageEvent adminEvent) {
		if (adminEvent.getType() != Type.ADMIN) {
            return null;
        }
		final String requestCommand = adminEvent.getAttribute(AdminCommand.ATT_COMMAND);
		if (this.isAdmin()) {
			if (!Utilities.resolveToBoolean(adminEvent.getAttribute(AdminCommand.ATT_UPDATE)) && 
					!Utilities.resolveToBoolean(adminEvent.getAttribute(AdminCommand.ATT_RESPONSE))) {
				return AdminCommand.composeResponse(requestCommand, true, false, "Operation not allowed: This user is also an admin!");
			}
		}
		final AdminCommand aCommand = this.commands.get(requestCommand);
		if (aCommand == null) {
            return AdminCommand.composeResponse(requestCommand, false, false, "Command not supported: " + requestCommand);
        }
		final String supported = adminEvent.getAttribute(AdminCommand.ATT_SUPPORTED);
		if (supported != null && !Utilities.resolveToBoolean(supported)) {
			SwingUtilities.invokeLater(() -> {
                final Application application = Application.getInstance();
                JOptionPane.showMessageDialog(application.getMainFrame(), aCommand.getName() + ": not supported for " + user.getPreferredName() + "!", "Command error", JOptionPane.ERROR_MESSAGE);
            });
			return null;
		}
		return aCommand.execute(user, adminEvent);
	}
	
	public boolean showLogin() {
		if (this.isLoginVisible()) {
            return false;
		}
		this.pw = new PasswordWizard(this.frame, "Administrative login", "Password",
				input -> {
					if (input != null) {
						this.setAdmin(true);
						this.frame.getAdminButton().setToolTipText("Logout");
						SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this.frame,
								"Login successful: You have been confirmed as administrator.",
								"Administrative login",
								JOptionPane.INFORMATION_MESSAGE));
					} else {
						this.frame.getAdminButton().setSelected(false);
					}
				}, password -> {
					try {
						return new ValidationResult<>(null, this.validate(password));
					} catch (final InvalidHashException e) {
						return new ValidationResult<>(e.getMessage(), false);
					}
				});
		this.pw.setVisible(true);
		return true;
	}
	
	public boolean isLoginVisible() {
		return this.pw != null && this.pw.isVisible();
	}
	
	/**
	 * @return whether this admin control is ready. (meaning the admin control knows the password hash to validate)
	 */
	public boolean isReady() {
		return this.hash != null;
	}
	
	/*
	 * Process all password stuff here
	 */
	
	private byte[] retrieveHash() {
		InputStream io = null;
		try {
			final URL url = new URL(Paths.WEBHOST_PASS);
			io = url.openStream();
			
			final byte[] hash = new byte[64];
			int read;
            int off = 0;
            while ((read = io.read(hash, off, hash.length - off)) > 0) {
                off += read;
            }
			return hash;
		} catch (final UnknownHostException ignored) {
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (io != null) {
				try {
					io.close();
				} catch (final IOException ignored) {}
			}
		}
		return null;
	}
	
	protected String refreshPassword() {
		final byte[] newHash = this.retrieveHash();
		if (newHash == null) {
            return null;
        }
		this.hash = newHash;
		return new String(this.hash, Application.CHARSET);
	}
	
	public boolean validate(final String password) throws InvalidHashException {
		this.frame.getStatusLabel().submit("Confirming password.", 0);
		this.refreshPassword();
		this.frame.getStatusLabel().setDefaultStatus();
		return confirm(this.hash, password);
	}
	
	protected static boolean confirm(final byte[] hash, final String input) throws InvalidHashException {
		if (hash == null) {
            throw new InvalidHashException("The password hash is null, it wasn't retrieved.");
        }
		return SHA2.verify(hash, SHA2.hash512Bytes(input));
	}
	
	public static class InvalidHashException extends Exception {
		
		private static final long serialVersionUID = 1L;
		
		public InvalidHashException(final String msg) {
			super(msg);
		}
		
	}
	
}