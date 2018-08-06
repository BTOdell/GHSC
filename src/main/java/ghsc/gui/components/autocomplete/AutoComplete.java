package ghsc.gui.components.autocomplete;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.UIResource;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import javax.swing.text.TextAction;

import ghsc.gui.components.autocomplete.workarounds.MacOSXPopupLocationFix;

@SuppressWarnings({"serial", "rawtypes"})
public final class AutoComplete {
	
	private static final List<String> COMBO_BOX_ACTIONS = Collections.unmodifiableList(Arrays.asList("selectNext", "selectNext2", "selectPrevious", "selectPrevious2", "pageDownPassThrough", "pageUpPassThrough", "homePassThrough", "endPassThrough"));
	
	private static final Object errorFeedbackAction = new TextAction("provide-error-feedback") {
		public void actionPerformed(final ActionEvent e) {
			UIManager.getLookAndFeel().provideErrorFeedback(this.getTextComponent(e));
		}
	};
	
	private AutoComplete() {
		// prevent instantiation
	}
	
	public static void enable(final JComboBox comboBox, final boolean strict) {
		enable(comboBox, null, strict);
	}
	
	public static void enable(final JComboBox comboBox, final ObjectToStringConverter converter, final boolean strict) {
		disable(comboBox);
		comboBox.setEditable(true);
		// fix the popup location
		MacOSXPopupLocationFix.install(comboBox);
		
		final JTextComponent editorComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
		final ACAdapter adapter = new ComboBoxAdapter(comboBox);
		final ACDocument document = createACDocument(adapter, strict, converter, editorComponent.getDocument());
		enable(editorComponent, document, adapter);
		editorComponent.addKeyListener(new KeyAdapter(comboBox));
		// set before adding the listener for the editor
		comboBox.setEditor(new ACComboBoxEditor(comboBox.getEditor(), document.getConverter()));
		// Changing the l&f can change the combobox' editor which in turn
		// would not be autocompletion-enabled. The new editor needs to be set-up.
		final PropertyChangeListener pcl = new PropertyChangeListener(comboBox);
		comboBox.addPropertyChangeListener("editor", pcl);
		comboBox.addPropertyChangeListener("enabled", pcl);
		
		if (!strict) {
			final ActionMap map = comboBox.getActionMap();
			for (final String key : COMBO_BOX_ACTIONS) {
				final Action a = map.get(key);
				map.put(key, new SelectionAction(a));
			}
		}
	}
	
	public static void enable(final JList list, final JTextComponent textComponent) {
		enable(list, textComponent, null);
	}
	
	public static void enable(final JList list, final JTextComponent textComponent, final ObjectToStringConverter stringConverter) {
		disable(list);
		final ACAdapter adapter = new ListAdapter(list, textComponent, stringConverter);
		final ACDocument document = createACDocument(adapter, true, stringConverter, textComponent.getDocument());
		enable(textComponent, document, adapter);
	}
	
	public static void enable(final JTextComponent textComponent, final List<?> items, final boolean strictMatching) {
		enable(textComponent, items, strictMatching, null);
	}
	
	public static void enable(final JTextComponent textComponent, final List<?> items, final boolean strictMatching, final ObjectToStringConverter stringConverter) {
		final ACAdapter adapter = new TextComponentAdapter(textComponent, items);
		final ACDocument document = createACDocument(adapter, strictMatching, stringConverter, textComponent.getDocument());
		enable(textComponent, document, adapter);
	}
	
	public static void enable(final JTextComponent textComponent, final ACDocument document, final ACAdapter adapter) {
		disable(textComponent);
		textComponent.setDocument(document);
		// mark entire text when the text component gains focus
		// otherwise the last mark would have been retained which is quiet confusing
		textComponent.addFocusListener(new AutoComplete.FocusAdapter(adapter));
		// Tweak some key bindings
		javax.swing.InputMap editorInputMap = textComponent.getInputMap();
		while (editorInputMap != null) {
			final javax.swing.InputMap parent = editorInputMap.getParent();
			if (parent instanceof UIResource) {
				installMap(editorInputMap, document.isStrictMatching());
				break;
			}
			editorInputMap = parent;
		}
		final ActionMap editorActionMap = textComponent.getActionMap();
		editorActionMap.put("nonstrict-backspace", new NonStrictBackspaceAction(editorActionMap.get(DefaultEditorKit.deletePrevCharAction), editorActionMap.get(DefaultEditorKit.selectionBackwardAction), adapter));
	}

	static void disable(final JComboBox comboBox) {
		final JTextComponent editorComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
		if (editorComponent.getDocument() instanceof ACDocument) {
			final ACDocument doc = (ACDocument) editorComponent.getDocument();
			if (doc.isStrictMatching()) {
				final ActionMap map = comboBox.getActionMap();
				for (final String key : COMBO_BOX_ACTIONS) {
					map.put(key, null);
				}
			}
			// remove old property change listener
			for (final java.beans.PropertyChangeListener l : comboBox.getPropertyChangeListeners("editor")) {
				if (l instanceof AutoComplete.PropertyChangeListener) {
					comboBox.removePropertyChangeListener("editor", l);
				}
			}
			for (final java.beans.PropertyChangeListener l : comboBox.getPropertyChangeListeners("enabled")) {
				if (l instanceof AutoComplete.PropertyChangeListener) {
					comboBox.removePropertyChangeListener("enabled", l);
				}
			}
			final ACComboBoxEditor editor = (ACComboBoxEditor) comboBox.getEditor();
			comboBox.setEditor(editor.wrapped);
			// remove old key listener
			for (final KeyListener l : editorComponent.getKeyListeners()) {
				if (l instanceof AutoComplete.KeyAdapter) {
					editorComponent.removeKeyListener(l);
					break;
				}
			}
			disable(editorComponent);
			for (final ActionListener l : comboBox.getActionListeners()) {
				if (l instanceof ComboBoxAdapter) {
					comboBox.removeActionListener(l);
					break;
				}
			}
			// TODO remove aqua fix
			// TODO reset editibility
		}
	}

	static void disable(final JList list) {
		for (final ListSelectionListener l : list.getListSelectionListeners()) {
			if (l instanceof ListAdapter) {
				list.removeListSelectionListener(l);
				break;
			}
		}
	}

	static void disable(final JTextComponent textComponent) {
		final Document doc = textComponent.getDocument();
		if (doc instanceof ACDocument) {
			// remove autocomplete key/action mappings
			javax.swing.InputMap map = textComponent.getInputMap();
			while (map.getParent() != null) {
				final javax.swing.InputMap parent = map.getParent();
				if (parent instanceof AutoComplete.InputMap) {
					map.setParent(parent.getParent());
				}
				map = parent;
			}
			textComponent.getActionMap().put("nonstrict-backspace", null);
			// remove old focus listener
			for (final FocusListener l : textComponent.getFocusListeners()) {
				if (l instanceof AutoComplete.FocusAdapter) {
					textComponent.removeFocusListener(l);
					break;
				}
			}
			// reset to original document
			textComponent.setDocument(((ACDocument) doc).getDelegate());
		}
	}
	
	private static void installMap(final javax.swing.InputMap componentMap, final boolean strict) {
		final InputMap map = new AutoComplete.InputMap();
		if (strict) {
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), DefaultEditorKit.selectionBackwardAction);
			// ignore VK_DELETE and CTRL+VK_X and beep instead when strict matching
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), errorFeedbackAction);
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), errorFeedbackAction);
		} else {
			// VK_BACKSPACE will move the selection to the left if the selected item is in the list
			// it will delete the previous character otherwise
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "nonstrict-backspace");
			// leave VK_DELETE and CTRL+VK_X as is
		}
		map.setParent(componentMap.getParent());
		componentMap.setParent(map);
	}
	
	static ACDocument createACDocument(final ACAdapter adapter, final boolean strictMatching, final ObjectToStringConverter stringConverter, final Document delegate) {
		if (delegate instanceof StyledDocument) {
			return new ACStyledDocument(adapter, strictMatching, stringConverter, (StyledDocument) delegate);
		}
		return new ACDocument(adapter, strictMatching, stringConverter, delegate);
	}

	static class NonStrictBackspaceAction extends TextAction {
		
		Action backspace;
		Action selectionBackward;
		ACAdapter adapter;

		public NonStrictBackspaceAction(final Action backspace, final Action selectionBackward, final ACAdapter adapter) {
			super("nonstrict-backspace");
			this.backspace = backspace;
			this.selectionBackward = selectionBackward;
			this.adapter = adapter;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (this.adapter.listContainsSelectedItem()) {
                this.selectionBackward.actionPerformed(e);
			} else {
                this.backspace.actionPerformed(e);
			}
		}
	}
	
	static class InputMap extends javax.swing.InputMap {
		private static final long serialVersionUID = 1L;
	}
	
	static class FocusAdapter extends java.awt.event.FocusAdapter {
		private final ACAdapter adapter;

		public FocusAdapter(final ACAdapter adaptor) {
			this.adapter = adaptor;
		}

		@Override
		public void focusGained(final FocusEvent e) {
            this.adapter.markAll();
		}
	}

	static class KeyAdapter extends java.awt.event.KeyAdapter {
		private final JComboBox comboBox;

		public KeyAdapter(final JComboBox comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void keyPressed(final KeyEvent e) {
			// don't popup on action keys (cursor movements, etc...)
			if (e.isActionKey()) {
				return;
			}
			// don't popup if the combobox isn't visible anyway
			if (this.comboBox.isDisplayable() && !this.comboBox.isPopupVisible()) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
					case KeyEvent.VK_ENTER: case KeyEvent.VK_ESCAPE:
						break;
					default:
                        this.comboBox.setPopupVisible(true);
				}
			}
		}
	}

	static class PropertyChangeListener implements java.beans.PropertyChangeListener {
		private final JComboBox comboBox;

		public PropertyChangeListener(final JComboBox comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			if ("editor".equals(evt.getPropertyName())) {
				this.handleEditor(evt);
			} else if ("enabled".equals(evt.getPropertyName())) {
				this.handleEnabled(evt);
			}
		}

		private void handleEnabled(final PropertyChangeEvent evt) {
			if (Boolean.TRUE.equals(evt.getNewValue())) {
                this.comboBox.setEditable(true);
			} else {
				final JTextComponent textComponent = (JTextComponent) this.comboBox.getEditor().getEditorComponent();
				final boolean strictMatching = ((ACDocument) textComponent.getDocument()).isStrictMatching();
                this.comboBox.setEditable(!strictMatching);
			}
		}

		private void handleEditor(final PropertyChangeEvent evt) {
			if (evt.getNewValue() instanceof ACComboBoxEditor) {
				return;
			}
			final ACComboBoxEditor acEditor = (ACComboBoxEditor) evt.getOldValue();
			boolean strictMatching = false;
			if (acEditor.getEditorComponent() != null) {
				final JTextComponent textComponent = (JTextComponent) acEditor.getEditorComponent();
				strictMatching = ((ACDocument) textComponent.getDocument()).isStrictMatching();
				disable(textComponent);
				for (final KeyListener l : textComponent.getKeyListeners()) {
					if (l instanceof KeyAdapter) {
						textComponent.removeKeyListener(l);
						break;
					}
				}
			}
			final JTextComponent editorComponent = (JTextComponent) this.comboBox.getEditor().getEditorComponent();
			final ACAdapter adapter = new ComboBoxAdapter(this.comboBox);
			final ACDocument document = createACDocument(adapter, strictMatching, acEditor.converter, editorComponent.getDocument());
			enable(editorComponent, document, adapter);
			editorComponent.addKeyListener(new AutoComplete.KeyAdapter(this.comboBox));
			// set before adding the listener for the editor
            this.comboBox.setEditor(new ACComboBoxEditor(this.comboBox.getEditor(), document.getConverter()));
		}
	}

	static class SelectionAction implements Action {
		private final Action delegate;

		public SelectionAction(final Action delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public void actionPerformed(final ActionEvent e) {
			final JComboBox comboBox = (JComboBox) e.getSource();
			final JTextComponent textComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
			final ACDocument doc = (ACDocument) textComponent.getDocument();
			// doing this prevents the updating of the selected item to "" during the remove prior
			// to the insert in JTextComponent.setText
			doc.strictMatching = true;
			try {
                this.delegate.actionPerformed(e);
			} finally {
				doc.strictMatching = false;
			}
		}
		
		@Override
		public void addPropertyChangeListener(final java.beans.PropertyChangeListener listener) {
            this.delegate.addPropertyChangeListener(listener);
		}
		
		@Override
		public void removePropertyChangeListener(final java.beans.PropertyChangeListener listener) {
            this.delegate.removePropertyChangeListener(listener);
		}
		
		@Override
		public Object getValue(final String key) {
			return this.delegate.getValue(key);
		}
		
		@Override
		public void putValue(final String key, final Object value) {
            this.delegate.putValue(key, value);
		}
		
		@Override
		public boolean isEnabled() {
			return this.delegate.isEnabled();
		}

		@Override
		public void setEnabled(final boolean b) {
            this.delegate.setEnabled(b);
		}
	}
	
}