package com.ghsc.gui.fileshare.internal;

import com.ghsc.gui.components.users.User;
import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.UUID;

public class RemotePackage extends FilePackage {
	
	private final User host;
    private final boolean passwordProtected;

    private RemoteFileNode[] roots;

    /**
     * Creates a new package from existing remote data.
     */
	public RemotePackage(final User user, final UUID uuid, final String name, final String description,
                         final Calendar creationDate, final Visibility visibility,
                         final boolean passwordProtected) {
		super(uuid, name, description, creationDate, visibility);
		this.host = user;
		this.passwordProtected = passwordProtected;
	}
	
	public User getHost() {
		return this.host;
	}
	
	@Override
	public String getOwner() {
		return this.host.getPreferredName();
	}
	
	@Override
	public boolean isPasswordProtected() {
		return this.passwordProtected;
	}
	
	public RemoteFileNode[] getRoots() {
		return this.roots;
	}
	
	public void setRoots(final RemoteFileNode[] roots) {
		this.roots = roots;
	}
	
	@Override
	public long getFileCount() {
		if (this.fileCount == null) {
			long temp = 0;
			for (final RemoteFileNode node : this.getRoots()) {
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
			for (final RemoteFileNode node : this.getRoots()) {
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
			for (final RemoteFileNode node : this.getRoots()) {
                temp += node.getSize();
            }
			this.size = temp;
		}
		return this.size;
	}
	
	public static RemotePackage parse(final User u, final String meta) {
		return parse(u, Tag.parse(meta));
	}
	
	public static RemotePackage parse(final User u, final Tag pTag) {
		// parse package info...
		if (pTag == null || !pTag.getName().equals(TAGNAME)) {
            return null;
        }
        final String uuidString = pTag.getAttribute(ATT_UUID);
        final String name = pTag.getAttribute(ATT_NAME);
        final String visibilityString = pTag.getAttribute(ATT_VISIBILITY);
        final String creationDateString = pTag.getAttribute(ATT_CREATIONDATE);
        if (uuidString == null || name == null || visibilityString == null || creationDateString == null) {
            return null;
        }
        final UUID uuid = UUID.fromString(uuidString);
        final Visibility visibility = Visibility.parse(visibilityString);
        final Calendar creationDate = parseCalendar(creationDateString);
        if (visibility == null || creationDate == null) {
            return null;
        }
		final RemotePackage parsedRemotePackage = new RemotePackage(
		        u, uuid, name, pTag.getAttribute(ATT_DESCRIPTION),
                creationDate, visibility, Utilities.resolveToBoolean(pTag.getAttribute(ATT_PASSWORDPROTECTED)));
        // Load download count
        final long downloadCount;
        final String downloadCountString = pTag.getAttribute(ATT_DOWNLOADCOUNT);
        if (downloadCountString != null) {
            downloadCount = Long.parseLong(downloadCountString);
        } else {
            downloadCount = 0;
        }
		parsedRemotePackage.setDownloadCount(downloadCount);
        // Load files
		final RemoteFileNodeChildren rChildren = new RemoteFileNodeChildren(null);
		final LinkedList<EndTaggable> tagStack = new LinkedList<>();
		tagStack.push(rChildren); // initialize with RemoteNodeChildren.
		final StringBuilder build = new StringBuilder(pTag.getPost());
		EndTaggable peek = rChildren;
		while (build.length() > 0) {
			if (Utilities.startsWith(build, peek.getEndTag())) { // build starts with endTag
				build.delete(0, peek.getEndTag().length());
				final Taggable popT = tagStack.pop();
				peek = tagStack.peek();
				if (peek != null) {
					peek.receive(popT);
				} else {
                    break;
                }
			} else {
				final Tag newTag = new Tag(build).parseBasic();
				if (newTag != null) {
					final EndTaggable resolve = peek.createForTag(newTag);
					if (resolve != null) {
						tagStack.push(resolve);
						peek = resolve;
						build.delete(0, newTag.getEncodedLength());
						continue;
					}
				}
				return null;
			}
		}
		final RemoteFileNode[] nodes = rChildren.toArray(new RemoteFileNode[rChildren.size()]);
		parsedRemotePackage.setRoots(nodes);
		return parsedRemotePackage;
	}
	
}