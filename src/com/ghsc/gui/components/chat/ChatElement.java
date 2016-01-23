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
	
	protected ChatElementList container = null;
	
	protected TimeStamp time;
	protected User sender = null;
	protected String sender_name, message, title;
	protected Color color;
	protected boolean automated = false, me = false, show = true;
	
	private int textLength = 0;
	
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
		
		init();
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
		
		init();
	}
	
	/**
	 * Creates for actual user.
	 */
	public ChatElement(ChatElementList container, TimeStamp time, User sender, String title, String message, boolean show, Color color) {
		this(container, time, title, message, false);
		
		this.sender = sender;
		this.color = color;
		this.show = show;
		
		init();
	}
	
	/**
	 * Hides this ChannelElement's text.
	 * @param hidden - whether to hide the text, or show it.
	 */
	public void setHidden(boolean hidden) {
		show = !hidden;
		select(0,0);
		refreshText();
	}
	
	public boolean isHidden() {
		return !show;
	}
	
	public User getUser() {
		return sender;
	}
	
	public boolean isMe() {
		return me;
	}
	
	/**
	 * Initializes this ChannelElement with chat data.
	 */
	private void init() {
		setBackground(null);
		
		setFont(Fonts.GLOBAL);
		setOpaque(true);
		setEditable(false);
		if (this.color != null)
			setForeground(this.color);
		configureStyles();
		refreshAll();
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				container.getChat().setSelection(ChatElement.this);
			}
		});
		
		Application.getInstance().getPopupManager().submit(new PopupBuilder() {
			public boolean build(Popup menu, PopupManager popupManager, Component sender, int x, int y) {
				final String selection = getSelectedText();
				if (selection != null) {
					JMenuItem fi = menu.createItem("Copy", new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copy();
						}
					});
					fi.setFont(Fonts.GLOBAL);
					menu.add(fi);
					if (message != null)
						menu.addSeparator();
				}
				if (message != null) {
					JMenuItem fi = menu.createItem(isHidden() ? "Show" : "Hide", new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							setHidden(!isHidden());
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
		pos = Math.max(0, Math.min(textLength - 1, pos));
		int prev = getCaretPosition();
		setCaretPosition(pos);
		insertIcon(new ImageIcon(img));
		setCaretPosition(prev);
	}
	
	protected void refreshSender() {
		if (me) {
			sender_name = Application.getInstance().getPreferredName();
		} else if (sender != null) {
			sender_name = this.sender.getPreferredName();
		}
	}
	
	protected void refreshText() {
		StringBuilder build = new StringBuilder();
		build.append("[");
		build.append(this.time.print(TimeStamp.Style.Hour12));
		build.append("] ");
		build.append(sender_name);
		if (!automated)
			build.append(" says");
		build.append(":");
		int bSize = build.length();
		if (title != null) {
			build.append(" ");
			build.append(title);
		}
		int lSize = build.length();
		boolean valid = show && message != null;
		if (valid) {
			build.append("\n");
			build.append(message);
		}
		textLength = build.length();
		setText(build.toString());
		
		StyledDocument sDoc = getStyledDocument();
		// apply styles
		sDoc.setCharacterAttributes(0, build.length(), emptyStyle, true);
		sDoc.setCharacterAttributes(0, bSize, boldStyle, true);
		if (valid) {
			// do the left indenting
			sDoc.setParagraphAttributes(lSize + 1, build.length(), indentStyle, false);
		}
	}
	
	protected void refreshAll() {
		refreshSender();
		refreshText();
	}
	
	private void configureStyles() {
		StyledDocument sDoc = getStyledDocument();
		
		indentStyle = sDoc.addStyle("li_style", sDoc.getStyle(StyleContext.DEFAULT_STYLE));
		StyleConstants.setLeftIndent(indentStyle, LEFT_INDENT);
		
		emptyStyle = new SimpleAttributeSet();
		boldStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(boldStyle, Fonts.GLOBAL.getFamily());
		StyleConstants.setFontSize(boldStyle, Fonts.GLOBAL.getSize());
		StyleConstants.setBold(boldStyle, true);
		
	}
	
}