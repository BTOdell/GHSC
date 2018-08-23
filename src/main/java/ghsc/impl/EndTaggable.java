package ghsc.impl;

import ghsc.util.Tag;

/**
 * TODO
 */
public interface EndTaggable extends Taggable {
	String getEndTag();
	EndTaggable createForTag(Tag tag);
}