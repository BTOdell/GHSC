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
		return event.getChange(elem);
	}
	
	@Override
	public Document getDocument() {
		return wrapped;
	}
	
	@Override
	public int getLength() {
		return event.getLength();
	}
	
	@Override
	public int getOffset() {
		return event.getOffset();
	}
	
	@Override
	public EventType getType() {
		return event.getType();
	}
	
}