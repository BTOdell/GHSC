package com.ghsc.gui.components.autocomplete;

import java.awt.Color;
import java.awt.Font;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

public class ACStyledDocument extends ACDocument implements StyledDocument {
	
	public ACStyledDocument(final ACAdapter adapter, final boolean strictMatching, final ObjectToStringConverter stringConverter, final StyledDocument delegate) {
		super(adapter, strictMatching, stringConverter, delegate);
	}

	public ACStyledDocument(final ACAdapter adapter, final boolean strictMatching, final ObjectToStringConverter stringConverter) {
		super(adapter, strictMatching, stringConverter);
	}

	public ACStyledDocument(final ACAdapter adapter, final boolean strictMatching) {
		super(adapter, strictMatching);
	}
	
	StyledDocument getDelegate() {
		return (StyledDocument) this.delegate;
	}

	@Override
	protected Document createDefaultDocument() {
		return new DefaultStyledDocument();
	}

	@Override
	public Style addStyle(final String nm, final Style parent) {
		return this.getDelegate().addStyle(nm, parent);
	}

	@Override
	public Color getBackground(final AttributeSet attr) {
		return this.getDelegate().getBackground(attr);
	}
	
	@Override
	public Element getCharacterElement(final int pos) {
		return this.getDelegate().getCharacterElement(pos);
	}
	
	@Override
	public Font getFont(final AttributeSet attr) {
		return this.getDelegate().getFont(attr);
	}
	
	@Override
	public Color getForeground(final AttributeSet attr) {
		return this.getDelegate().getForeground(attr);
	}
	
	@Override
	public Style getLogicalStyle(final int p) {
		return this.getDelegate().getLogicalStyle(p);
	}

	@Override
	public Element getParagraphElement(final int pos) {
		return this.getDelegate().getParagraphElement(pos);
	}

	@Override
	public Style getStyle(final String nm) {
		return this.getDelegate().getStyle(nm);
	}
	
	@Override
	public void removeStyle(final String nm) {
		this.getDelegate().removeStyle(nm);
	}
	
	@Override
	public void setCharacterAttributes(final int offset, final int length, final AttributeSet s, final boolean replace) {
		this.getDelegate().setCharacterAttributes(offset, length, s, replace);
	}
	
	@Override
	public void setLogicalStyle(final int pos, final Style s) {
		this.getDelegate().setLogicalStyle(pos, s);
	}
	
	@Override
	public void setParagraphAttributes(final int offset, final int length, final AttributeSet s, final boolean replace) {
		this.getDelegate().setParagraphAttributes(offset, length, s, replace);
	}
	
}