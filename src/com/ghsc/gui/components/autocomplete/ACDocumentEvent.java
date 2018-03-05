package com.ghsc.gui.components.autocomplete;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.Element;

final class ACDocumentEvent implements DocumentEvent {
	
	private final Document wrapped;
	private final DocumentEvent event;

	public ACDocumentEvent(final Document wrapped, final DocumentEvent event) {
		this.wrapped = wrapped;
		this.event = event;
	}

	@Override
	public ElementChange getChange(Element elem) {
		return this.event.getChange(elem);
	}
	
	@Override
	public Document getDocument() {
		return this.wrapped;
	}
	
	@Override
	public int getLength() {
		return this.event.getLength();
	}
	
	@Override
	public int getOffset() {
		return this.event.getOffset();
	}
	
	@Override
	public EventType getType() {
		return this.event.getType();
	}
	
}