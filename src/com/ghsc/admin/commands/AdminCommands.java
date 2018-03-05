package com.ghsc.admin.commands;

import java.util.HashMap;

import com.ghsc.admin.AdminControl;
import com.ghsc.admin.commands.flash.FlashCommand;
import com.ghsc.admin.commands.kick.KickCommand;

/**
 * Commands:<br>
 * 1. "Kick" (kicks a user from a channel)<br>
 * 2. "Flash" (flashes the entire screen)<br>
 * Future commands:<br>
 * 1. Force update<br>
 * 2. Send (stream) update<br>
 * 3. Shutdown program<br>
 * 4. Delete program<br>
 * 5. View file system anonymously<br>
 * 6. View screen anonymously<br>
 * 7. Ban user and unban user<br>
 * 8. Prevent user shutdown<br>
 * 9. ...
 * @author Odell
 */
public class AdminCommands {
	
	private AdminControl control;
	private final HashMap<String, AdminCommand> commands = new HashMap<String, AdminCommand>();
	
	public AdminCommands(AdminControl control) {
		this.control = control;
		this.init();
	}
	
	private void init() {
		AdminCommand command;
		
		command = new FlashCommand(this.control);
		this.commands.put(command.getTag(), command);
		
		command = new KickCommand(this.control);
		this.commands.put(command.getTag(), command);
		
	}
	
	public final AdminCommand get(String name) {
		if (name == null) {
            return null;
        }
		return this.commands.get(name);
	}
	
}