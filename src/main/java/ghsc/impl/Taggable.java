package ghsc.impl;

import ghsc.util.Tag;

/**
 * TODO
 */
public interface Taggable {
	String getTagName();
	void receive(Object o);
	Taggable createForTag(Tag tag);
}