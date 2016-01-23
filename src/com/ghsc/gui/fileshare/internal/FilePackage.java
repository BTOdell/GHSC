package com.ghsc.gui.fileshare.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.regex.Pattern;

import com.ghsc.gui.Application;
import com.ghsc.gui.components.chat.Chat;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.fileshare.components.PackagePanel;
import com.ghsc.impl.Filter;
import com.ghsc.util.Utilities;

public abstract class FilePackage {
	
	public static class Visibility implements Filter<FilePackage> {
		
		public enum Type {
			PUBLIC("pu"), // visible to all users
			PRIVATE("pr"), // must be unlocked with "key"
			CHANNEL("c"), // only available to users in channel(s)
			USER("u"); // only available to select users (could be used for friends)
			
			private final String identifier;
			
			Type(String identifier) {
				this.identifier = identifier;
			}
			
			public String getIdentifier() {
				return identifier;
			}
			
			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder(super.toString().toLowerCase());
				sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
				return sb.toString();
			}
			
			public static Type match(String identifier) {
				for (final Type v : values()) {
					if (v.getIdentifier().equals(identifier))
						return v;
				}
				return null;
			}
			
		}
		
		private Type type;
		private Object data;
		private boolean privateDiscovered = false;
		
		public Visibility(Object raw) {
			if (raw != null) {
				String rawStr = raw.toString();
				if (rawStr != null) {
					final int vIndex = rawStr.indexOf(':');
					if (vIndex >= 0) {
						this.data = rawStr.substring(vIndex + 1, rawStr.length());
						rawStr = rawStr.substring(0, vIndex);
					}
					this.type = Visibility.Type.match(rawStr);
					return;
				}
			}
			throw new IllegalArgumentException("Raw object data cannot be null!");
		}
		
		public Visibility(Type type, Object data) {
			this.type = type;
			this.data = data;
		}
		
		public Type getType() {
			return type;
		}
		
		public void setType(Type t) {
			this.type = t;
		}
		
		public Object getData() {
			return data;
		}
		
		public void setData(Object data) {
			this.data = data;
		}
		
		/**
		 * @return whether or not this private package has been discovered. Will return true if package is not private.
		 */
		public boolean isDiscovered() {
			return type != Type.PRIVATE || privateDiscovered;
		}
		
		/**
		 * Sets whether this private package has been discovered. Only applies for private packages.
		 */
		public void setDiscovered(boolean pd) {
			this.privateDiscovered = pd;
		}

		@Override
		public boolean accept(FilePackage p) {
			if (p instanceof LocalPackage || type == Type.PUBLIC || p.isActive()) // handles public packages
				return true;
			if (p instanceof RemotePackage) {
				RemotePackage rp = (RemotePackage) p;
				if (!isDiscovered()) // handles private packages
					return false;
				String[] dataStr = data.toString().split(Pattern.quote(","));
				switch (type) {
					case CHANNEL: // handles channel packages
						final Chat[] chats = rp.getHost().getContainer().getMainFrame().getChatContainer().getAll();
						for (Chat chat : chats) {
							if (chat == null)
								continue;
							if (chat instanceof Channel && Utilities.contains(((Channel) chat).getName(), dataStr))
								return true;
						}
						
						// TODO: Is this fall through intentional !
						// adding break until this question is resolved.
						break;
						
					case USER: // handles user packages
						final ArrayList<String> uuids = new ArrayList<String>();
						for (int i = 0; i < dataStr.length; i++) {
							final String d = dataStr[i];
							final int dI = d.indexOf('|');
							if (dI >= 0) {
								uuids.add(d.substring(dI + 1, d.length()));
							}
						}
						return Utilities.contains(Application.getInstance().getID().toString(), uuids.toArray(new String[uuids.size()]));
					case PRIVATE:
						// TODO: Not done yet.
						break;
					case PUBLIC:
						// TODO: Not done yet.
						break;
					default:
						break;
				}
			}
			return false;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(type.getIdentifier());
			if (data != null) {
				sb.append(":");
				sb.append(data);
			}
			return sb.toString();
		}
		
	}
	
	static Calendar parseCalendar(SimpleDateFormat sdf, String cal) {
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(sdf.parse(cal));
		} catch (ParseException e) {
			return null;
		}
		return c;
	}
	
	public static final String TAGNAME = "p", ATT_NAME = "n", ATT_DESCRIPTION = "d", ATT_CREATIONDATE = "c", 
			ATT_DOWNLOADCOUNT = "dc", ATT_ACTIVE = "a", ATT_PRIVATEKEY = "pk", ATT_PASSWORDPROTECTED = "p", ATT_VISIBILITY = "v", ATT_UUID = "u";
	protected static final String DATE_FORMAT = "MM/dd/yy hh:mm aa";
	
	String name = null, description = null;
	Calendar creationDate;
	String creationDateString;
	UUID uuid = null;
	Visibility visibility;
	Long size = null, fileCount = null, directoryCount = null, downloadCount = 0L;
	boolean active = true;
	
	FilePackage(final String name, final String description, final Calendar creationDate, final Visibility visibility) {
		this.name = name;
		this.description = description;
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		this.creationDate = creationDate;
		this.creationDateString = sdf.format(creationDate.getTime());
		this.visibility = visibility;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String d) {
		this.description = d;
	}
	
	public Calendar getCreationDate() {
		return creationDate;
	}
	
	/**
	 * MM/dd/yy hh:mm AM|PM
	 */
	public String getCreationDateString() {
		return creationDateString;
	}
	
	public Visibility getVisibility() {
		return visibility;
	}
	
	public void setVisibility(Visibility vis) {
		this.visibility = vis;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * @return how many unique requests were made this this package.
	 */
	public long getDownloadCount() {
		return downloadCount;
	}
	
	public void setDownloadCount(long downloads) {
		this.downloadCount = downloads;
	}
	
	public abstract String getOwner();
	
	/**
	 * @return whether this package is password protected.
	 */
	public abstract boolean isPasswordProtected();
	
	/**
	 * @return how many files are in this package.
	 */
	public abstract long getFileCount();
	
	/**
	 * @return how many directories are in this package.
	 */
	public abstract long getDirectoryCount();
	
	public abstract UUID getUUID();
	
	/**
	 * Calculates the size of all the files in this package.<br>
	 * Warning: This function can be a expensive operation if the package contains numerous files.
	 * @return the sum of the sizes of all the files.
	 */
	public abstract long getSize();
	
	@Override
	public boolean equals(Object o) {
		if (o != null) {
			if (o instanceof FilePackage) {
				return this == o;
			} else if (o instanceof PackagePanel) {
				return this == ((PackagePanel) o).getPackage();
			}
		}
		return false;
	}
	
}