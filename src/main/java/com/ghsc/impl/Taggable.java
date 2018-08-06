package com.ghsc.impl;

import com.ghsc.util.Tag;

public interface Taggable {
	String getTagName();
	void receive(Object o);
	Taggable createForTag(Tag tag);
}