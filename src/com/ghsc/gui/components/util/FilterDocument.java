package com.ghsc.gui.components.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import com.ghsc.impl.Filter;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class FilterDocument extends DefaultStyledDocument {
	
	private static final long serialVersionUID = 1L;

	private final Filter<Character> filter;
	
	public FilterDocument(final Filter<Character> filter) {
		super();
		this.filter = filter;
	}
	
	@Override
	public void insertString(int offset, String str, AttributeSet as) throws BadLocationException {
		final StringBuilder sb = new StringBuilder(str);
		int i = 0;
		while (i < sb.length()) {
			char c = sb.charAt(i);
			if (filter != null && !filter.accept(c)) {
				sb.deleteCharAt(i);
				continue;
			}
			i++;
		}
		if (sb.length() > 0) {
			super.insertString(offset, sb.toString(), as);
		}
	}
	
}