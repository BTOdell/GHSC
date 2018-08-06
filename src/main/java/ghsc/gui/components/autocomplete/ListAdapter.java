package ghsc.gui.components.autocomplete;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

public class ListAdapter extends ACAdapter implements ListSelectionListener {
	
	JList<?> list;
	JTextComponent textComponent;
	ObjectToStringConverter stringConverter;

	public ListAdapter(final JList<?> list, final JTextComponent textComponent) {
		this(list, textComponent, ObjectToStringConverter.DEFAULT);
	}

	public ListAdapter(final JList<?> list, final JTextComponent textComponent, final ObjectToStringConverter stringConverter) {
		this.list = list;
		this.textComponent = textComponent;
		this.stringConverter = stringConverter;
		
		list.addListSelectionListener(this);
	}

	@Override
	public void valueChanged(final ListSelectionEvent listSelectionEvent) {
		this.getTextComponent().setText(this.stringConverter.getPreferredStringForItem(this.list.getSelectedValue()));
		this.markAll();
	}

	@Override
	public Object getSelectedItem() {
		return this.list.getSelectedValue();
	}

	@Override
	public int getItemCount() {
		return this.list.getModel().getSize();
	}

	@Override
	public Object getItem(final int index) {
		return this.list.getModel().getElementAt(index);
	}

	@Override
	public void setSelectedItem(final Object item) {
        this.list.setSelectedValue(item, true);
	}

	@Override
	public JTextComponent getTextComponent() {
		return this.textComponent;
	}
	
}