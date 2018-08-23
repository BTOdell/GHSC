package ghsc.gui.components.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.util.function.Predicate;

/**
 * TODO
 */
public class FilterDocument extends DefaultStyledDocument {
	
	private static final long serialVersionUID = 1L;

	private final Predicate<Character> predicate;
	
	public FilterDocument(final Predicate<Character> predicate) {
		super();
		this.predicate = predicate;
	}
	
	@Override
	public void insertString(final int offset, final String str, final AttributeSet as) throws BadLocationException {
		final StringBuilder sb = new StringBuilder(str);
		int i = 0;
		while (i < sb.length()) {
			final char c = sb.charAt(i);
			if (this.predicate != null && !this.predicate.test(c)) {
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