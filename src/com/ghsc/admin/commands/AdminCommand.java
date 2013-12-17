package com.ghsc.admin.commands;

import com.ghsc.admin.AdminControl;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.components.users.User;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

/**
 * Tags: c, u, r, suc, m, sup, custom<br>
 * 'c' (command): the command that the message is referring to.<br>
 * 'u' (update): true or false, true if the message is to inform everyone that a command caused a state change, false if this isn't.<br>
 * 'r' (response): strictly should be sent back to the user who sent the command, true if this is a response to a command, false if it isn't.<br>
 * 'sup' (supported): should only exist if response is true, tells weather the command attempt is supported.
 * 'suc' (success): should only exist if response is true and supported is true, tells whether the command was successful or not.<br>
 * 'm' (message): should only exist if response is true and supported is true, returns a user-end message of the current state after the command finished.<br>
 * Custom: any command related data which should be passed to the command.<br>
 * 
 * Standard formats<br>
 * Send command: "command", command, "update", false, "response", false, "custom", ...<br>
 * Send update: "command", command, "update", true, "response", false, "custom", ...<br>
 * Send response: "command", command, "update", false, "response", true, "supported", true/false, ["success", true/false,] "message", message, "custom", ...<br>
 * 
 * @author Odell
 */
public abstract class AdminCommand {
	
	public static final String ATT_COMMAND = "c", ATT_UPDATE = "u", ATT_RESPONSE = "r", ATT_SUPPORTED = "sp", ATT_SUCCESS = "s", ATT_MESSAGE = "m";
	
	protected AdminControl control;
	
	public AdminCommand(AdminControl control) {
		this.control = control;
	}
	
	/**
	 * Will execute this command on another thread.
	 */
	public abstract Tag execute(final User user, final MessageEvent me);
	/**
	 * The displayed name of this command.
	 */
	public abstract String getName();
	/**
	 * The protocol tag name of this command, used for MessageEvents.
	 */
	public abstract String getTag();
	
	/*
	 * MessageEvent composing for admin commands.
	 */
	
	public static Tag composeCommand(Object command, Object... custom) {
		return Tag.construct(Type.ADMIN, Utilities.merge(custom, ATT_COMMAND, command, ATT_UPDATE, Utilities.resolveToString(false), ATT_RESPONSE, Utilities.resolveToString(false)));
	}
	
	public static Tag composeUpdate(Object command, Object... custom) {
		return Tag.construct(Type.ADMIN, Utilities.merge(custom, ATT_COMMAND, command, ATT_UPDATE, Utilities.resolveToString(true), ATT_RESPONSE, Utilities.resolveToString(false)));
	}
	
	public static Tag composeResponse(Object command, boolean supported, boolean success, Object message, Object... custom) {
		if (supported) {
			return Tag.construct(Type.ADMIN, Utilities.merge(custom, ATT_COMMAND, command, ATT_UPDATE, Utilities.resolveToString(false), ATT_RESPONSE, Utilities.resolveToString(true), ATT_SUPPORTED, Utilities.resolveToString(supported), ATT_SUCCESS, Utilities.resolveToString(success), ATT_MESSAGE, message));
		} else {
			return Tag.construct(Type.ADMIN, Utilities.merge(custom, ATT_COMMAND, command, ATT_UPDATE, Utilities.resolveToString(false), ATT_RESPONSE, Utilities.resolveToString(true), ATT_SUPPORTED, Utilities.resolveToString(supported), ATT_MESSAGE, message));
		}
	}
	
}