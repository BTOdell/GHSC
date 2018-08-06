package com.ghsc.gui.components.autocomplete;

import static com.ghsc.gui.components.autocomplete.ObjectToStringConverter.DEFAULT;

import java.util.Comparator;

import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;

public class ACDocument implements Document {

	private static final Comparator<String> EQUALS_IGNORE_CASE = (o1, o2) -> o1.equalsIgnoreCase(o2) ? 0 : -1;
	private static final Comparator<String> STARTS_WITH_IGNORE_CASE = (o1, o2) -> {
		if (o1.length() < o2.length()) {
			return -1;
		}
		return o1.regionMatches(true, 0, o2, 0, o2.length()) ? 0 : -1;
	};
	private static final Comparator<String> EQUALS = (o1, o2) -> o1.equals(o2) ? 0 : -1;
	private static final Comparator<String> STARTS_WITH = (o1, o2) -> o1.startsWith(o2) ? 0 : -1;

	protected final Document delegate;
	private final Handler handler;
	ACAdapter adapter;
	private final ObjectToStringConverter converter;
	protected boolean strictMatching;
	boolean selecting;

	public ACDocument(final ACAdapter adapter, final boolean strictMatching, final ObjectToStringConverter converter, final Document delegate) {
		if (adapter == null) {
			throw new IllegalArgumentException("adapter cannot be null");
		}
		this.adapter = adapter;
		this.strictMatching = strictMatching;
		this.converter = converter == null ? DEFAULT : converter;
		this.delegate = delegate == null ? this.createDefaultDocument() : delegate;
		
		this.handler = new Handler();
		this.delegate.addDocumentListener(this.handler);
		if (this.isStrictMatching()) {
			// Handle initially selected object
			final Object selected = adapter.getSelectedItem();
			if (selected != null) {
				final String itemStr = this.converter.getPreferredStringForItem(selected);
                this.setText(itemStr);
				this.adapter.setSelectedItemString(itemStr);
			}
		}
		this.adapter.markAll();
	}
	
	public ACDocument(final ACAdapter adapter, final boolean strictMatching, final ObjectToStringConverter stringConverter) {
		this(adapter, strictMatching, stringConverter, null);
	}
	
	public ACDocument(final ACAdapter adapter, final boolean strictMatching) {
		this(adapter, strictMatching, null);
	}
	
	Document getDelegate() {
		return this.delegate;
	}
	
	ObjectToStringConverter getConverter() {
		return this.converter;
	}

	protected Document createDefaultDocument() {
		return new PlainDocument();
	}

	@Override
	public void remove(final int offs, final int len) throws BadLocationException {
		// return immediately when selecting an item
		if (this.selecting) {
			return;
		}
        this.getDelegate().remove(offs, len);
		if (!this.strictMatching) {
            this.setSelectedItem(this.getText(0, this.getLength()), this.getText(0, this.getLength()));
			this.adapter.getTextComponent().setCaretPosition(offs);
		}
	}

	@Override
	public void insertString(int offs, final String str, final AttributeSet a) throws BadLocationException {
		// return immediately when selecting an item
		if (this.selecting) {
			return;
		}
		// insert the string into the document
        this.getDelegate().insertString(offs, str, a);
		// lookup and select a matching item
		final LookupResult lookupResult;
		final String pattern = this.getText(0, this.getLength());
		if (pattern == null || pattern.isEmpty()) {
			lookupResult = new LookupResult(null, "");
            this.setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
		} else {
			lookupResult = this.lookupItem(pattern);
		}
		if (lookupResult.matchingItem != null) {
            this.setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
		} else {
			if (this.strictMatching) {
				// keep old item selected if there is no match
				lookupResult.matchingItem = this.adapter.getSelectedItem();
				lookupResult.matchingString = this.adapter.getSelectedItemString();
				// imitate no insert (later on offs will be incremented by
				// str.length(): selection won't move forward)
				offs = str == null ? offs : offs - str.length();
				if (str != null && !str.isEmpty()) {
					// provide feedback to the user that his input has been received but can not be accepted
					UIManager.getLookAndFeel().provideErrorFeedback(this.adapter.getTextComponent());
				}
			} else {
				// no item matches => use the current input as selected item
				lookupResult.matchingItem = this.getText(0, this.getLength());
				lookupResult.matchingString = this.getText(0, this.getLength());
                this.setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
			}
		}
        this.setText(lookupResult.matchingString);
		// select the completed part
		final int len = str == null ? 0 : str.length();
		offs = lookupResult.matchingString == null ? 0 : offs + len;
		this.adapter.markFrom(offs);
	}
	
	private void setText(final String text) {
		try {
			// remove all text and insert the completed string
            this.getDelegate().remove(0, this.getLength());
            this.getDelegate().insertString(0, text, null);
		} catch (final BadLocationException e) {
			throw new RuntimeException(e.toString());
		}
	}
	
	private void setSelectedItem(final Object item, final String itemAsString) {
		this.selecting = true;
		this.adapter.setSelectedItem(item);
		this.adapter.setSelectedItemString(itemAsString);
		this.selecting = false;
	}
	
	private LookupResult lookupItem(final String pattern) {
		final Object selectedItem = this.adapter.getSelectedItem();
		LookupResult lookupResult;
		// first try: case sensitive
		lookupResult = this.lookupItem(pattern, EQUALS);
		if (lookupResult != null) {
			return lookupResult;
		}
		lookupResult = this.lookupOneItem(selectedItem, pattern, STARTS_WITH);
		if (lookupResult != null) {
			return lookupResult;
		}
		lookupResult = this.lookupItem(pattern, STARTS_WITH);
		if (lookupResult != null) {
			return lookupResult;
		}
		// second try: ignore case
		lookupResult = this.lookupItem(pattern, EQUALS_IGNORE_CASE);
		if (lookupResult != null) {
			return lookupResult;
		}
		lookupResult = this.lookupOneItem(selectedItem, pattern, STARTS_WITH_IGNORE_CASE);
		if (lookupResult != null) {
			return lookupResult;
		}
		lookupResult = this.lookupItem(pattern, STARTS_WITH_IGNORE_CASE);
		if (lookupResult != null) {
			return lookupResult;
		}
		// no item starts with the pattern => return null
		return new LookupResult(null, "");
	}

	private LookupResult lookupOneItem(final Object item, final String pattern, final Comparator<String> comparator) {
		final String[] possibleStrings = this.converter.getPossibleStringsForItem(item);
		if (possibleStrings != null) {
			for (final String possibleString : possibleStrings) {
				if (comparator.compare(possibleString, pattern) == 0) {
					return new LookupResult(item, possibleString);
				}
			}
		}
		return null;
	}

	private LookupResult lookupItem(final String pattern, final Comparator<String> comparator) {
		// iterate over all items and return first match
		for (int i = 0, n = this.adapter.getItemCount(); i < n; i++) {
			final Object currentItem = this.adapter.getItem(i);
			final LookupResult result = this.lookupOneItem(currentItem, pattern, comparator);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	@Override
	public void addDocumentListener(final DocumentListener listener) {
		this.handler.addDocumentListener(listener);
	}
	
	@Override
	public void addUndoableEditListener(final UndoableEditListener listener) {
		this.handler.addUndoableEditListener(listener);
	}
	
	@Override
	public Position createPosition(final int offs) throws BadLocationException {
		return this.delegate.createPosition(offs);
	}
	
	@Override
	public Element getDefaultRootElement() {
		return this.getDelegate().getDefaultRootElement();
	}
	
	@Override
	public Position getEndPosition() {
		return this.getDelegate().getEndPosition();
	}
	
	@Override
	public int getLength() {
		return this.getDelegate().getLength();
	}
	
	@Override
	public Object getProperty(final Object key) {
		return this.getDelegate().getProperty(key);
	}
	
	@Override
	public Element[] getRootElements() {
		return this.getDelegate().getRootElements();
	}
	
	@Override
	public Position getStartPosition() {
		return this.getDelegate().getStartPosition();
	}
	
	@Override
	public String getText(final int offset, final int length) throws BadLocationException {
		return this.getDelegate().getText(offset, length);
	}
	
	@Override
	public void getText(final int offset, final int length, final Segment txt) throws BadLocationException {
        this.getDelegate().getText(offset, length, txt);
	}
	
	@Override
	public void putProperty(final Object key, final Object value) {
        this.getDelegate().putProperty(key, value);
	}
	
	@Override
	public void removeDocumentListener(final DocumentListener listener) {
		this.handler.removeDocumentListener(listener);
	}
	
	@Override
	public void removeUndoableEditListener(final UndoableEditListener listener) {
		this.handler.removeUndoableEditListener(listener);
	}
	
	@Override
	public void render(final Runnable r) {
        this.getDelegate().render(r);
	}
	
	public boolean isStrictMatching() {
		return this.strictMatching;
	}
	
	private static class LookupResult {
		Object matchingItem;
		String matchingString;

		public LookupResult(final Object matchingItem, final String matchingString) {
			this.matchingItem = matchingItem;
			this.matchingString = matchingString;
		}
	}
	
	private class Handler implements DocumentListener, UndoableEditListener {
		
		private final EventListenerList listenerList = new EventListenerList();

		public void addDocumentListener(final DocumentListener listener) {
			this.listenerList.add(DocumentListener.class, listener);
		}

		public void addUndoableEditListener(final UndoableEditListener listener) {
			this.listenerList.add(UndoableEditListener.class, listener);
		}
		
		public void removeDocumentListener(final DocumentListener listener) {
			this.listenerList.remove(DocumentListener.class, listener);
		}
		
		public void removeUndoableEditListener(final UndoableEditListener listener) {
			this.listenerList.remove(UndoableEditListener.class, listener);
		}
		
		@Override
		public void changedUpdate(DocumentEvent e) {
			e = new ACDocumentEvent(ACDocument.this, e);
			// Guaranteed to return a non-null array
			final Object[] listeners = this.listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DocumentListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((DocumentListener) listeners[i + 1]).changedUpdate(e);
				}
			}
		}
		
		@Override
		public void insertUpdate(DocumentEvent e) {
			e = new ACDocumentEvent(ACDocument.this, e);
			// Guaranteed to return a non-null array
			final Object[] listeners = this.listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DocumentListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((DocumentListener) listeners[i + 1]).insertUpdate(e);
				}
			}
		}
		
		@Override
		public void removeUpdate(DocumentEvent e) {
			e = new ACDocumentEvent(ACDocument.this, e);
			// Guaranteed to return a non-null array
			final Object[] listeners = this.listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DocumentListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((DocumentListener) listeners[i + 1]).removeUpdate(e);
				}
			}
		}
		
		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			e = new UndoableEditEvent(ACDocument.this, e.getEdit());
			// Guaranteed to return a non-null array
			final Object[] listeners = this.listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == UndoableEditListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((UndoableEditListener) listeners[i + 1]).undoableEditHappened(e);
				}
			}
		}
		
	}
	
}