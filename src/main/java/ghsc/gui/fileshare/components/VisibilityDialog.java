package ghsc.gui.fileshare.components;

import ghsc.common.Fonts;
import ghsc.event.EventListener;
import ghsc.gui.components.autocomplete.AutoComplete;
import ghsc.gui.components.autocomplete.ObjectToStringConverter;
import ghsc.impl.InputVerifier;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * TODO
 * @param <E>
 */
public class VisibilityDialog<E> extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private final PackageWizard wizard;
	private final Collection<E> available;
    private final Collection<E> contents;
	private final EventListener<ArrayList<E>> callback;
	private final InputVerifier<E> inputVerifier;
	private final ObjectToStringConverter converter;
	private final boolean strict;

    private ACComboBox entryBox;
	private DefaultComboBoxModel<E> comboModel;
	private JScrollPane scrollPane;
	private JList<E> list;
	private DefaultListModel<E> listModel;
	private JButton cancelButton;
	private JButton okButton;

	/**
	 * Create the dialog.
	 */
	public VisibilityDialog(final PackageWizard wizard, final Collection<E> available, final Collection<E> contents,
                            final EventListener<ArrayList<E>> callback, final InputVerifier<E> inputVerifier,
                            final ObjectToStringConverter converter, final boolean strict) {
		super(wizard);
		this.wizard = wizard;
		this.available = available;
		this.contents = contents;
		this.callback = callback;
		this.inputVerifier = inputVerifier;
		this.converter = converter;
		this.strict = strict;
		
		Iterator<E> it = this.contents.iterator();
		while (it.hasNext()) {
            this.getListModel().addElement(it.next());
		}
		it = this.available.iterator();
		while (it.hasNext()) {
			final E e = it.next();
			if (this.contents.contains(e)) {
                continue;
            }
            this.getComboModel().addElement(e);
		}

        this.initComponents();
	}
	
	private void close() {
        this.callback.eventReceived(null);
        this.dispose();
	}
	
	private void initComponents() {
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setTitle(this.wizard.getTitle() + " - Edit visibility arguments");
        this.setSize(325, 400);
        this.setMinimumSize(this.getSize());
        this.setLocationRelativeTo(this.wizard);

        final JPanel contentPane = new JPanel();
		final GroupLayout groupLayout = new GroupLayout(contentPane);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(this.getScrollPane(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
						.addComponent(this.getEntryBox(), Alignment.LEADING, 0, 264, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(this.getOkButton(), GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(this.getCancelButton(), GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(this.getEntryBox(), GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(this.getScrollPane(), GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(this.getCancelButton())
						.addComponent(this.getOkButton()))
					.addContainerGap())
		);
        contentPane.setLayout(groupLayout);
        this.setContentPane(contentPane);
	}
	
	public ACComboBox getEntryBox() {
		if (this.entryBox == null) {
            this.entryBox = new ACComboBox(this.getComboModel());
		}
		return this.entryBox;
	}
	
	public DefaultComboBoxModel<E> getComboModel() {
		if (this.comboModel == null) {
            this.comboModel = new DefaultComboBoxModel<>();
		}
		return this.comboModel;
	}
	
	public JScrollPane getScrollPane() {
		if (this.scrollPane == null) {
            this.scrollPane = new JScrollPane();
            this.scrollPane.setViewportView(this.getList());
		}
		return this.scrollPane;
	}
	
	public JList<E> getList() {
		if (this.list == null) {
            this.list = new JList<>(this.getListModel());
            this.list.addKeyListener(new KeyAdapter() {
				public void keyPressed(final KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						final E selected = VisibilityDialog.this.getList().getSelectedValue();
						if (selected != null) {
							if (VisibilityDialog.this.getListModel().removeElement(selected) &&
                                    VisibilityDialog.this.available.contains(selected)) {
                                VisibilityDialog.this.getComboModel().addElement(selected);
							}
						}
					}
				}
			});
		}
		return this.list;
	}
	
	public DefaultListModel<E> getListModel() {
		if (this.listModel == null) {
            this.listModel = new DefaultListModel<>();
		}
        return this.listModel;
    }

    public JButton getOkButton() {
        if (this.okButton == null) {
            this.okButton = new JButton("OK");
            this.okButton.addActionListener(e -> {
                final int size = this.getListModel().size();
                final ArrayList<E> a = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    a.add(this.getListModel().get(i));
                }
                this.callback.eventReceived(a);
                this.dispose();
            });
        }
		return this.okButton;
	}
	
	public JButton getCancelButton() {
		if (this.cancelButton == null) {
            this.cancelButton = new JButton("Cancel");
            this.cancelButton.addActionListener(e -> this.close());
		}
		return this.cancelButton;
	}
	
	public class ACComboBox extends JComboBox<E> {
		
		private static final long serialVersionUID = 1L;
		
		private ACComboBox(final ComboBoxModel<E> model) {
			super(model);
            this.initComponents();
			AutoComplete.enable(this, VisibilityDialog.this.converter, VisibilityDialog.this.strict);
		}

		private void initComponents() {
            this.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
				public void keyPressed(final KeyEvent e) {
					if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                        return;
                    }
                    @SuppressWarnings("unchecked")
					final E selected = (E) ACComboBox.this.getSelectedItem();
					System.out.println("Selected: " + selected);
					final E verified = VisibilityDialog.this.inputVerifier.verify(selected);
					System.out.println("Verified: " + selected);
                    VisibilityDialog.this.getComboModel().removeElement(verified);
                    VisibilityDialog.this.getListModel().addElement(verified);
					ACComboBox.this.setSelectedItem(null);
                    VisibilityDialog.this.getList().setSelectedValue(verified, true);
				}
			});
            this.setFont(Fonts.GLOBAL);
            this.setEditable(true);
            this.setDoubleBuffered(true);
		}
		
		@Override
		public void setModel(final ComboBoxModel<E> model) {
			super.setModel(model);
			if (!VisibilityDialog.this.strict) {
                this.setSelectedIndex(-1);
			}
		}
		
	}
	
}