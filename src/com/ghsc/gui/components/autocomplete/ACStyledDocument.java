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
	
	public ACStyledDocument(ACAdapter adapter, boolean strictMatching, ObjectToStringConverter stringConverter, StyledDocument delegate) {
		super(adapter, strictMatching, stringConverter, delegate);
	}

	public ACStyledDocument(ACAdapter adapter, boolean strictMatching, ObjectToStringConverter stringConverter) {
		super(adapter, strictMatching, stringConverter);
	}

	public ACStyledDocument(ACAdapter adapter, boolean strictMatching) {
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
	public Style addStyle(String nm, Style parent) {
		return this.getDelegate().addStyle(nm, parent);
	}

	@Override
	public Color getBackground(AttributeSet attr) {
		return this.getDelegate().getBackground(attr);
	}
	
	@Override
	public Element getCharacterElement(int pos) {
		return this.getDelegate().getCharacterElement(pos);
	}
	
	@Override
	public Font getFont(AttributeSet attr) {
		return this.getDelegate().getFont(attr);
	}
	
	@Override
	public Color getForeground(AttributeSet attr) {
		return this.getDelegate().getForeground(attr);
	}
	
	@Override
	public Style getLogicalStyle(int p) {
		return this.getDelegate().getLogicalStyle(p);
	}

	@Override
	public Element getParagraphElement(int pos) {
		return this.getDelegate().getParagraphElement(pos);
	}

	@Override
	public Style getStyle(String nm) {
		return this.getDelegate().getStyle(nm);
	}
	
	@Override
	public void removeStyle(String nm) {
		this.getDelegate().removeStyle(nm);
	}
	
	@Override
	public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace) {
		this.getDelegate().setCharacterAttributes(offset, length, s, replace);
	}
	
	@Override
	public void setLogicalStyle(int pos, Style s) {
		this.getDelegate().setLogicalStyle(pos, s);
	}
	
	@Override
	public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {
		this.getDelegate().setParagraphAttributes(offset, length, s, replace);
	}
	
}