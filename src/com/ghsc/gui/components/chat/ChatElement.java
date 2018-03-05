package com.ghsc.gui.components.chat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JTextPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import com.ghsc.common.Colors;
import com.ghsc.common.Fonts;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.popup.Popup;
import com.ghsc.gui.components.popup.PopupBuilder;
import com.ghsc.gui.components.popup.PopupManager;
import com.ghsc.gui.components.users.User;
import com.ghsc.util.TimeStamp;

/**
 * Used to display a message/element in a Chat list model.
 * @author Odell
 */
public class ChatElement extends JTextPane {
	
	private static final long serialVersionUID = 1L;
	
	protected ChatElementList container;
	
	protected TimeStamp time;
	protected User sender;
	protected String sender_name, message, title;
	protected Color color;
	protected boolean automated, me, show = true;
	
	private int textLength;
	
	/*
	 * Styles
	 */
	private static final float LEFT_INDENT = 10.0F;
	MutableAttributeSet emptyStyle, boldStyle, indentStyle;
	
	protected ChatElement(ChatElementList container, TimeStamp time, String title, String message, boolean me) {
		super();
		this.container = container;
		this.time = time;
		this.title = title;
		this.message = message;
		this.me = me;
	}
	
	/**
	 * Creates for my messages.
	 */
	public ChatElement(ChatElementList container, TimeStamp time, String title, String message) {
		this(container, time, title, message, true);

		this.init();
	}
	
	/**
	 * Creates for automated message.
	 */
	public ChatElement(ChatElementList container, TimeStamp time, String sender, String title, String message) {
		this(container, time, sender, title, message, Colors.MESSAGE_BLUE);
	}
	
	public ChatElement(ChatElementList container, TimeStamp time, String sender, String title, String message, Color color) {
		this(container, time, title, message, false);
		this.color = color;
		this.automated = true;
		this.sender_name = sender;

		this.init();
	}
	
	/**
	 * Creates for actual user.
	 */
	public ChatElement(ChatElementList container, TimeStamp time, User sender, String title, String message, boolean show, Color color) {
		this(container, time, title, message, false);
		
		this.sender = sender;
		this.color = color;
		this.show = show;

		this.init();
	}
	
	/**
	 * Hides this ChannelElement's text.
	 * @param hidden - whether to hide the text, or show it.
	 */
	public void setHidden(boolean hidden) {
		this.show = !hidden;
		this.select(0,0);
		this.refreshText();
	}
	
	public boolean isHidden() {
		return !this.show;
	}
	
	public User getUser() {
		return this.sender;
	}
	
	public boolean isMe() {
		return this.me;
	}
	
	/**
	 * Initializes this ChannelElement with chat data.
	 */
	private void init() {
		this.setBackground(null);

		this.setFont(Fonts.GLOBAL);
		this.setOpaque(true);
		this.setEditable(false);
		if (this.color != null) {
			this.setForeground(this.color);
        }
		this.configureStyles();
		this.refreshAll();

		this.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				ChatElement.this.container.getChat().setSelection(ChatElement.this);
			}
		});
		
		Application.getInstance().getPopupManager().submit(new PopupBuilder() {
			public boolean build(Popup menu, PopupManager popupManager, Component sender, int x, int y) {
				final String selection = ChatElement.this.getSelectedText();
				if (selection != null) {
					JMenuItem fi = menu.createItem("Copy", new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							ChatElement.this.copy();
						}
					});
					fi.setFont(Fonts.GLOBAL);
					menu.add(fi);
					if (ChatElement.this.message != null) {
                        menu.addSeparator();
                    }
				}
				if (ChatElement.this.message != null) {
					JMenuItem fi = menu.createItem(ChatElement.this.isHidden() ? "Show" : "Hide", new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							ChatElement.this.setHidden(!ChatElement.this.isHidden());
						}
					});
					fi.setFont(Fonts.GLOBAL);
					menu.add(fi);
				}
				return menu.getComponentCount() > 0;
			}
		}, this);
	}
	
	public void insertImage(Image img, int pos) {
		pos = Math.max(0, Math.min(this.textLength - 1, pos));
		int prev = this.getCaretPosition();
		this.setCaretPosition(pos);
		this.insertIcon(new ImageIcon(img));
		this.setCaretPosition(prev);
	}
	
	protected void refreshSender() {
		if (this.me) {
			this.sender_name = Application.getInstance().getPreferredName();
		} else if (this.sender != null) {
			this.sender_name = this.sender.getPreferredName();
		}
	}
	
	protected void refreshText() {
		StringBuilder build = new StringBuilder();
		build.append("[");
		build.append(this.time.print(TimeStamp.Style.Hour12));
		build.append("] ");
		build.append(this.sender_name);
		if (!this.automated) {
            build.append(" says");
        }
		build.append(":");
		int bSize = build.length();
		if (this.title != null) {
			build.append(" ");
			build.append(this.title);
		}
		int lSize = build.length();
		boolean valid = this.show && this.message != null;
		if (valid) {
			build.append("\n");
			build.append(this.message);
		}
		this.textLength = build.length();
		this.setText(build.toString());
		
		StyledDocument sDoc = this.getStyledDocument();
		// apply styles
		sDoc.setCharacterAttributes(0, build.length(), this.emptyStyle, true);
		sDoc.setCharacterAttributes(0, bSize, this.boldStyle, true);
		if (valid) {
			// do the left indenting
			sDoc.setParagraphAttributes(lSize + 1, build.length(), this.indentStyle, false);
		}
	}
	
	protected void refreshAll() {
		this.refreshSender();
		this.refreshText();
	}
	
	private void configureStyles() {
		StyledDocument sDoc = this.getStyledDocument();

		this.indentStyle = sDoc.addStyle("li_style", sDoc.getStyle(StyleContext.DEFAULT_STYLE));
		StyleConstants.setLeftIndent(this.indentStyle, LEFT_INDENT);

		this.emptyStyle = new SimpleAttributeSet();
		this.boldStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(this.boldStyle, Fonts.GLOBAL.getFamily());
		StyleConstants.setFontSize(this.boldStyle, Fonts.GLOBAL.getSize());
		StyleConstants.setBold(this.boldStyle, true);
		
	}
	
}