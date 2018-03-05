package com.ghsc.gui.fileshare.internal;

import com.ghsc.gui.Application;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.net.encryption.SHA2;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.UUID;

public class LocalPackage extends FilePackage {
	
	private final String privateKey;

	private LocalFileNode[] roots;
	private String password;
	private byte[] passwordHash;
	
	/**
	 * Creates a new package.
	 */
	public LocalPackage(final String name, final String description, final Calendar creationDate, final Visibility visibility) {
		super(UUID.randomUUID(), name, description, creationDate, visibility);
		this.privateKey = FileShare.generatePrivateKey();
	}

    /**
     * Create from existing package data.
     */
	public LocalPackage(final UUID uuid, final String name, final String description,
						final Calendar creationDate, final Visibility visibility,
						final String privateKey) {
		super(uuid, name, description, creationDate, visibility);
		this.privateKey = privateKey;
	}
	
	@Override
	public String getOwner() {
		return Application.getInstance().getPreferredName();
	}
	
	public LocalFileNode getFile(final String relativePath) {
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
	
	public void setPassword(final String password) {
		this.password = password;
		this.passwordHash = password != null ? SHA2.hash512Bytes(password) : null;
	}
	
	public boolean verifyPassword(final String password) {
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
	
	/*
	 * Remote package related methods
	 */
	
	/**
	 * Converts this LocalPackage to meta data to be interpreted by a RemotePackage.
	 * @return the meta data for a RemotePackage.
	 */
	public String toRemoteMeta() {
		final LinkedList<Object> ll = new LinkedList<>();
		ll.add(ATT_NAME);
		ll.add(this.getName());
		final String description = this.getDescription();
		if (description != null) {
			ll.add(ATT_DESCRIPTION);
			ll.add(description);
		}
		ll.add(ATT_CREATIONDATE);
		ll.add(this.getCreationDateString());
		ll.add(ATT_DOWNLOADCOUNT);
		ll.add(this.getDownloadCount());
		if (this.isActive()) {
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
		final StringBuilder build = new StringBuilder().append(Tag.construct(TAGNAME, ll.toArray()).getEncodedString());
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
		final LinkedList<Object> ll = new LinkedList<>();
		ll.add(ATT_NAME);
		ll.add(this.getName());
        final String description = this.getDescription();
		if (description != null) {
			ll.add(ATT_DESCRIPTION);
			ll.add(description);
		}
		ll.add(ATT_CREATIONDATE);
		ll.add(this.getCreationDateString());
		ll.add(ATT_DOWNLOADCOUNT);
		ll.add(this.getDownloadCount());
		if (this.isActive()) {
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
		final StringBuilder build = new StringBuilder().append(Tag.construct(TAGNAME, ll.toArray()).getEncodedString());
		for (final LocalFileNode root : this.roots) {
            build.append(root.toSaveMeta());
        }
		build.append("</").append(FileNodeChildren.TAGNAME).append(">");
		build.append("</").append(TAGNAME).append(">");
		return build.toString();
	}
	
	public static LocalPackage parseSaveMeta(final String meta) {
		final Tag pTag = Tag.parse(meta);
		if (pTag == null || !pTag.getName().equals(TAGNAME)) {
            return null;
        }
        final String uuidString = pTag.getAttribute(ATT_UUID);
        final String name = pTag.getAttribute(ATT_NAME);
        final String visibilityString = pTag.getAttribute(ATT_VISIBILITY);
        final String creationDateString = pTag.getAttribute(ATT_CREATIONDATE);
        final String privateKey = pTag.getAttribute(ATT_PRIVATEKEY);
        if (uuidString == null || name == null || visibilityString == null || creationDateString == null || privateKey == null) {
            return null;
        }
        final UUID uuid = UUID.fromString(uuidString);
        final Visibility visibility = Visibility.parse(visibilityString);
        final Calendar creationDate = parseCalendar(creationDateString);
        if (visibility == null || creationDate == null) {
            return null;
        }
        final LocalPackage parsedLocalPackage = new LocalPackage(uuid, name, pTag.getAttribute(ATT_DESCRIPTION), creationDate, visibility, privateKey);
        parsedLocalPackage.setPassword(pTag.getAttribute(ATT_PASSWORDPROTECTED));
        parsedLocalPackage.setActive(pTag.getAttribute(ATT_ACTIVE) != null);
        // Load download count
        final long downloadCount;
        final String downloadCountString = pTag.getAttribute(ATT_DOWNLOADCOUNT);
        if (downloadCountString != null) {
            downloadCount = Long.parseLong(downloadCountString);
        } else {
            downloadCount = 0;
        }
        parsedLocalPackage.setDownloadCount(downloadCount);
        // Load files
		final LocalFileNodeChildren lChildren = new LocalFileNodeChildren(null);
		final LinkedList<EndTaggable> tagStack = new LinkedList<>();
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
		final LocalFileNode[] nodes = lChildren.toArray(new LocalFileNode[lChildren.size()]);
        parsedLocalPackage.setRoots(nodes);
		return parsedLocalPackage;
	}
	
}