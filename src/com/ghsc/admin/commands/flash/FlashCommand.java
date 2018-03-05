package com.ghsc.admin.commands.flash;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.ghsc.admin.AdminControl;
import com.ghsc.admin.commands.AdminCommand;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.User;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

public class FlashCommand extends AdminCommand {
	
	public static final String TAG = "flash";
	public static final String ATT_STATE = "st";
	public static final String ATT_ENABLE = "e";
	
	private FlashFrame frame;
	
	public FlashCommand(final AdminControl control) {
		super(control);
	}

	@Override
	public Tag execute(final User user, final MessageEvent me) {
		if (Utilities.resolveToBoolean(me.getAttribute(ATT_RESPONSE))) {
			final boolean success = Utilities.resolveToBoolean(me.getAttribute(ATT_SUCCESS));
			final String message = me.getAttribute(ATT_MESSAGE);
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(Application.getInstance().getMainFrame(),
					message,
					"Command result",
					success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE));
			return null;
		} else if (Utilities.resolveToBoolean(me.getAttribute(ATT_UPDATE))) {
			user.setCommandState(TAG, Utilities.resolveToBoolean(me.getAttribute(ATT_STATE))); // state
			return null;
		} else {
			final boolean enable = Utilities.resolveToBoolean(me.getAttribute(ATT_ENABLE)); // enable
			if (enable) {
				if (this.frame == null) {
                    this.frame = new FlashFrame();
                    this.frame.setVisible(true);
					new Thread(() -> user.getContainer().send(AdminCommand.composeUpdate(TAG, ATT_STATE, Utilities.resolveToString(true)), User.ALL)).start();
					return AdminCommand.composeResponse(TAG, true, true, this.getName() + ": enabled");
				}
			} else {
				if (this.frame != null) {
                    this.frame.dispose();
                    this.frame = null;
					new Thread(() -> user.getContainer().send(AdminCommand.composeUpdate(TAG, ATT_STATE, Utilities.resolveToString(false)), User.ALL)).start();
					return AdminCommand.composeResponse(TAG, true, true, this.getName() + ": disabled");
				}
			}
			return AdminCommand.composeResponse(TAG, true, false, this.getName() + ": Bad command state.");
		}
	}
	
	@Override
	public String getName() {
		return "Flash";
	}
	
	@Override
	public String getTag() {
		return TAG;
	}

}