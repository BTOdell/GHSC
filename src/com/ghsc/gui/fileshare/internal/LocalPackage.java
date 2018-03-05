package com.ghsc.gui.fileshare.internal;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.UUID;

import com.ghsc.gui.Application;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.net.encryption.SHA2;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

public class LocalPackage extends FilePackage {
	
	private final String privateKey;
	private LocalFileNode[] roots;
	private String password;
	private byte[] passwordHash;
	
	/**
	 * Creates a new package.
	 */
	public LocalPackage(final String name, final String description, final Calendar creationDate, final Visibility visibility, final String password, final LocalFileNode[] nodes) {
		super(name, description, creationDate, visibility);
		this.privateKey = FileShare.generatePrivateKey();
		this.setPassword(password);
		this.roots = nodes;
	}
	
	/**
	 * Parse constructor.
	 */
	private LocalPackage(final String name, final String description, final String creationDateS, final Visibility visibility, final String uuid, final String privateKey, final String password, final boolean active) {
		super(name, description, parseCalendar(new SimpleDateFormat(DATE_FORMAT), creationDateS), visibility);
		try {
			this.uuid = UUID.fromString(uuid);
		} catch (IllegalArgumentException iae) {
			this.uuid = null;
		}
		this.privateKey = privateKey;
		this.setPassword(password);
		this.active = active;
	}
	
	@Override
	public String getOwner() {
		return Application.getInstance().getPreferredName();
	}
	
	public LocalFileNode getFile(String relativePath) {
		return LocalFileNode.findFile(relativePath, Arrays.asList(this.roots));
	}
	
	public String getPrivateKey() {
		return this.privateKey;
	}
	
	@Override
	public boolean isPasswordProtected() {
		return this.passwordHash != null;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public void setPassword(String password) {
		this.password = password;
		this.passwordHash = password != null ? SHA2.hash512Bytes(password) : null;
	}
	
	public boolean verifyPassword(String password) {
		return !this.isPasswordProtected() || SHA2.verify(this.passwordHash, SHA2.hash512Bytes(password));
	}
	
	public LocalFileNode[] getRoots() {
		return this.roots;
	}
	
	public void setRoots(final LocalFileNode... roots) {
		this.roots = roots;
	}
	
	@Override
	public long getFileCount() {
		if (this.fileCount == null) {
			long temp = 0;
			for (final LocalFileNode node : this.getRoots()) {
                temp += node.getFileCount();
            }
			this.fileCount = temp;
		}
		return this.fileCount;
	}

	@Override
	public long getDirectoryCount() {
		if (this.directoryCount == null) {
			long temp = 0;
			for (final LocalFileNode node : this.getRoots()) {
                temp += node.getDirectoryCount();
            }
			this.directoryCount = temp;
		}
		return this.directoryCount;
	}
	
	@Override
	public long getSize() {
		if (this.size == null) {
			long temp = 0;
			for (final LocalFileNode node : this.getRoots()) {
                temp += node.getSize();
            }
			this.size = temp;
		}
		return this.size;
	}
	
	/**
	 * Calculates the UUID (universally unique identification) for this Package.
	 * @return the latest UUID.
	 */
	@Override
	public UUID getUUID() {
		if (this.uuid == null) {
			/*
			final StringBuilder sb = new StringBuilder();
			sb.append(Application.getLocalAddress().getHostAddress());
			sb.append(name);
			for (LocalFileNode f : getRoots()) {
				sb.append(f.concat());
			}
			uuid = UUID.nameUUIDFromBytes(sb.toString().getBytes(Application.CHARSET));
			*/
			this.uuid = UUID.randomUUID();
		}
		return this.uuid;
	}
	
	/*
	 * Remote package related methods
	 */
	
	/**
	 * Converts this LocalPackage to meta data to be interpreted by a RemotePackage.
	 * @return the meta data for a RemotePackage.
	 */
	public String toRemoteMeta() {
		final LinkedList<Object> ll = new LinkedList<Object>();
		ll.add(ATT_NAME);
		ll.add(this.name);
		if (this.description != null) {
			ll.add(ATT_DESCRIPTION);
			ll.add(this.description);
		}
		ll.add(ATT_CREATIONDATE);
		ll.add(this.getCreationDateString());
		ll.add(ATT_DOWNLOADCOUNT);
		ll.add(this.getDownloadCount());
		if (this.active) {
			ll.add(ATT_ACTIVE);
			ll.add(Utilities.resolveToBoolean(true));
		}
		if (this.password != null) {
			ll.add(ATT_PASSWORDPROTECTED);
			ll.add(Utilities.resolveToBoolean(true));
		}
		ll.add(ATT_VISIBILITY);
		ll.add(this.getVisibility());
		ll.add(ATT_UUID);
		ll.add(this.getUUID());
		StringBuilder build = new StringBuilder().append(Tag.construct(TAGNAME, ll.toArray()).getEncodedString());
		for (final LocalFileNode root : this.roots) {
            build.append(root.toRemoteMeta());
        }
		build.append("</").append(FileNodeChildren.TAGNAME).append(">");
		build.append("</").append(TAGNAME).append(">");
		return build.toString();
	}
	
	public String toRemoteUpdateMeta() {
		// TODO: determine if this is the best way to do this, if so, implement
		return null;
	}
	
	/*
	 * Save related methods
	 */
	
	public String toSaveMeta() {
		final LinkedList<Object> ll = new LinkedList<Object>();
		ll.add(ATT_NAME);
		ll.add(this.name);
		if (this.description != null) {
			ll.add(ATT_DESCRIPTION);
			ll.add(this.description);
		}
		ll.add(ATT_CREATIONDATE);
		ll.add(this.getCreationDateString());
		ll.add(ATT_DOWNLOADCOUNT);
		ll.add(this.getDownloadCount());
		if (this.active) {
			ll.add(ATT_ACTIVE);
			ll.add(Utilities.resolveToBoolean(true));
		}
		ll.add(ATT_PRIVATEKEY);
		ll.add(this.privateKey);
		if (this.password != null) {
			ll.add(ATT_PASSWORDPROTECTED);
			ll.add(this.password);
		}
		ll.add(ATT_VISIBILITY);
		ll.add(this.getVisibility());
		ll.add(ATT_UUID);
		ll.add(this.getUUID());
		StringBuilder build = new StringBuilder().append(Tag.construct(TAGNAME, ll.toArray()).getEncodedString());
		for (final LocalFileNode root : this.roots) {
            build.append(root.toSaveMeta());
        }
		build.append("</").append(FileNodeChildren.TAGNAME).append(">");
		build.append("</").append(TAGNAME).append(">");
		return build.toString();
	}
	
	public static LocalPackage parseSaveMeta(String meta) {
		final Tag pTag = Tag.parse(meta);
		if (pTag == null) {
            return null;
        }
		String name = null, uuid = null, vA = null, cD = null;
		if ((!pTag.getName().equals(TAGNAME)) || (name = pTag.getAttribute(ATT_NAME)) == null || 
			(uuid = pTag.getAttribute(ATT_UUID)) == null || (vA = pTag.getAttribute(ATT_VISIBILITY)) == null ||
			(cD = pTag.getAttribute(ATT_CREATIONDATE)) == null) {
            return null;
        }
		final LocalPackage lp = new LocalPackage(name, pTag.getAttribute(ATT_DESCRIPTION), cD, new Visibility(vA), uuid, 
				pTag.getAttribute(ATT_PRIVATEKEY), pTag.getAttribute(ATT_PASSWORDPROTECTED), pTag.getAttribute(ATT_ACTIVE) != null);
		lp.setDownloadCount(Long.parseLong(pTag.getAttribute(ATT_DOWNLOADCOUNT)));
		
		final LocalFileNodeChildren lChildren = new LocalFileNodeChildren(null);
		final LinkedList<EndTaggable> tagStack = new LinkedList<EndTaggable>();
		tagStack.push(lChildren);
		final StringBuilder content = new StringBuilder(pTag.getPost());
		EndTaggable peek = lChildren;
		while (content.length() > 0) {
			if (Utilities.startsWith(content, peek.getEndTag())) {
				content.delete(0, peek.getEndTag().length());
				final Taggable popT = tagStack.pop();
				peek = tagStack.peek();
				if (peek != null) {
					peek.receive(popT);
				} else {
                    break;
                }
			} else {
				final Tag newTag = new Tag(content).parseBasic();
				if (newTag != null) {
					final EndTaggable resolve = peek.createForTag(newTag);
					if (resolve != null) {
						tagStack.push(resolve);
						peek = resolve;
						content.delete(0, newTag.getEncodedLength());
						continue;
					}
				}
				return null;
			}
		}
		LocalFileNode[] nodes = lChildren.toArray(new LocalFileNode[lChildren.size()]);
		lp.setRoots(nodes);
		return lp;
	}
	
}