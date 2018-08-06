package ghsc.gui.components.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import ghsc.impl.Filter;

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
	public void insertString(final int offset, final String str, final AttributeSet as) throws BadLocationException {
		final StringBuilder sb = new StringBuilder(str);
		int i = 0;
		while (i < sb.length()) {
			final char c = sb.charAt(i);
			if (this.filter != null && !this.filter.accept(c)) {
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