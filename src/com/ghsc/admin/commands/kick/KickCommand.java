package com.ghsc.admin.commands.kick;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.ghsc.admin.AdminControl;
import com.ghsc.admin.commands.AdminCommand;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.chat.ChatContainer;
import com.ghsc.gui.components.users.User;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

public class KickCommand extends AdminCommand {
	
	public static final String TAG = "kick";
	public static final String ATT_CHANNEL = "c";
	
	public KickCommand(final AdminControl control) {
		super(control);
	}

	@Override
	public Tag execute(final User user, final MessageEvent me) {
		if (Utilities.resolveToBoolean(me.getAttribute(ATT_RESPONSE))) {
			final boolean success = Utilities.resolveToBoolean(me.getAttribute(ATT_SUCCESS));
			if (!success) {
				final String message = me.getAttribute(ATT_MESSAGE);
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(Application.getInstance().getMainFrame(), message, "Command not successful", JOptionPane.ERROR_MESSAGE));
			}
			return null;
		} else if (Utilities.resolveToBoolean(me.getAttribute(ATT_UPDATE))) {
			return null;
		} else {
			final String channel = me.getAttribute(ATT_CHANNEL);
			if (channel != null) {
				final ChatContainer cc = Application.getInstance().getMainFrame().getChatContainer();
				cc.remove(cc.getChat(channel));
				return null;
			}
			return AdminCommand.composeResponse(TAG, true, false, this.getName() + ": User is not in this channel.");
		}
	}
	
	@Override
	public String getName() {
		return "Kick";
	}
	
	@Override
	public String getTag() {
		return TAG;
	}

}