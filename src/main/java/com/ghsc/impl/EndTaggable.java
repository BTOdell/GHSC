package com.ghsc.impl;

import com.ghsc.util.Tag;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public interface EndTaggable extends Taggable {
	String getEndTag();
	EndTaggable createForTag(Tag tag);
}