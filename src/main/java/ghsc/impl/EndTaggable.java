package ghsc.impl;

import ghsc.util.Tag;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public interface EndTaggable extends Taggable {
	String getEndTag();
	EndTaggable createForTag(Tag tag);
}