package com.ghsc.event.message;

import com.ghsc.util.Tag;

/**
 * A container/wrapper for transferring all data between applications.
 * @author Odell
 */
public class MessageEvent extends Tag {
	
	protected Type type;
	
	/**
	 * Indicates the type of MessageEvent.
	 * @author Odell
	 */
	public enum Type {
		
		// multicast socket
		/**
		 * p ip="..." n="..."
		 * ping ip="..." nick="..."
		 */
		PING ("p"),
		
		// user sockets
		/**
		 * i h="..." n="..."
		 * identify hostname="..." nick="..."
		 */
		IDENTIFY ("i"),
		/**
		 * j c="..."
		 * join channel="..."
		 */
		JOIN ("j"),
		/**
		 * l c="..."
		 * leave channel="..."
		 */
		LEAVE ("l"),
		/**
		 * m c="..." data
		 * message channel="..." data
		 */
		MESSAGE("m"),
		/**
		 * a r="t|f" co="..." cu="..." etc etc data
		 * admin response="true|false" command="..." custom="..." etc etc data
		 */
		ADMIN ("a"),
		/**
		 * f t="n|e|r"
		 * fileshare type="new|edit|remove"
		 */
		FILE_SHARE ("f");
		
		String match;
		
		Type(final String m) {
			this.match = m;
		}
		
		@Override
		public String toString() {
			return this.match;
		}
		
		public static Type from(final String type) {
			for (final Type t : values()) {
				if (t.match.equalsIgnoreCase(type)) {
                    return t;
                }
			}
			return null;
		}
		
	}
	
	/**
	 * Initializes a new MessageEvent.
	 * @param raw the raw data that this MessageEvent is made up of.
	 */
	private MessageEvent(final Object raw) {
		super(raw);
	}
	
	/**
	 * @return the type of this MessageEvent.
	 */
	public final Type getType() {
		return this.type;
	}
	
	/**
	 * {@inheritDoc Tag#parse()}
	 */
	@Override
	public MessageEvent parse() {
		return (MessageEvent) super.parse();
	}
	
	/**
	 * Parses raw text to a MessageEvent.</br>
	 * Will return 'null' if this function didn't parse the text correctly.
	 * @param data - the text to parse.
	 * @return a MessageEvent if the text parsed correctly, otherwise 'null'.
	 */
	public static MessageEvent parse(final Object data) {
		final MessageEvent event = new MessageEvent(data).parse();
		if (event != null) {
			final Type t = Type.from(event.getName());
			if (t != null) {
				event.type = t;
			} else {
				return null;
			}
		}
		return event;
	}
	
	/**
	 * {@inheritDoc Tag#parseBasic()}
	 */
	@Override
	public MessageEvent parseBasic() {
		return (MessageEvent) super.parseBasic();
	}
	
	/**
	 * Parses raw text to a MessageEvent.</br>
	 * Will return 'null' if this function didn't parse the text correctly.
	 * @param data - the text to parse.
	 * @return a MessageEvent if the text parsed correctly, otherwise 'null'.
	 */
	public static MessageEvent parseBasic(final Object data) {
		final MessageEvent event = new MessageEvent(data).parseBasic();
		if (event != null) {
			final Type t = Type.from(event.getName());
			if (t != null) {
                event.type = t;
            } else {
                return null;
            }
		}
		return event;
	}
	
}