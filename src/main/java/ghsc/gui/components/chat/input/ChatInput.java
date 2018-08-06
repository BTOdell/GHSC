package ghsc.gui.components.chat.input;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import ghsc.common.Fonts;
import ghsc.event.EventProvider;
import ghsc.event.IEventProvider;
import ghsc.gui.MainFrame;
import ghsc.gui.components.util.PromptHandler;

public class ChatInput extends JTextArea {
	
	private static final long serialVersionUID = 1L;

	private final EventProvider<String> sendMessageProvider = new EventProvider<>();

	private final MainFrame frame;

	public ChatInput(final MainFrame frame) {
		this.frame = frame;
		this.init();
	}
	
	private void init() {

		this.setTransferHandler(new TransferHandler());
		this.setDropMode(DropMode.INSERT);
		this.setEnabled(false);
		this.setFont(Fonts.GLOBAL);
		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (e.isShiftDown()) {
						ChatInput.this.insert("\n", ChatInput.this.getCaretPosition());
					} else {
						ChatInput.this.sendMessage();
						e.consume();
					}
				}
			}
		});
		this.setDoubleBuffered(true);
		this.setColumns(10);
		new PromptHandler(this, "Type something...", Color.GRAY, JLabel.TOP);

		this.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(final DocumentEvent arg0) {
				this.updateLineCount();
			}
			public void insertUpdate(final DocumentEvent arg0) {
				this.updateLineCount();
			}
			public void removeUpdate(final DocumentEvent arg0) {
				this.updateLineCount();
			}
			private void updateLineCount() {
                final int lineCount = ChatInput.this.getLineCount();
                if (lineCount <= 5) {
					ChatInput.this.setRows(lineCount);
                    ChatInput.this.frame.getChatPanel().revalidate();
                }
            }
		});
	}

    /**
     * Gets the event provider for sent messages.
     */
	public IEventProvider<String> getEventProvider() {
	    return this.sendMessageProvider;
    }

	public void sendMessage() {
	    this.sendMessageProvider.fireEvent(this.getText());
	}

	private class TransferHandler extends javax.swing.TransferHandler {
		
		private static final long serialVersionUID = 1L;

		public TransferHandler() {
			super();
		}
		
		public boolean canImport(final TransferHandler.TransferSupport support) {
			//chatFocusGained();
			return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
		}
		
		public boolean importData(final TransferHandler.TransferSupport support) {
			try {
				final DataFlavor[] supportedFlavors = support.getDataFlavors();
				for (final DataFlavor flavor : supportedFlavors) {
					System.out.println("Flavor: " + flavor);
					if (DataFlavor.stringFlavor.equals(flavor)) {
						final BufferedReader read = new BufferedReader(flavor.getReaderForText(support.getTransferable()));
						final StringBuilder build = new StringBuilder();
						String line;
						while ((line = read.readLine()) != null) {
							build.append(line);
						}
						final String text = build.toString();
						final JTextComponent.DropLocation dLoc = ChatInput.this.getDropLocation();
						if (dLoc != null) {
							final int index = dLoc.getIndex();
							ChatInput.this.insert(text, index);
							ChatInput.this.setCaretPosition(index + text.length());
						} else {
							ChatInput.this.insert(text, ChatInput.this.getCaretPosition());
						}
						//check();
						break;
					} else if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
						// TODO: paste image!
					}
				}
				System.out.println();
				return super.importData(support);
			} catch (UnsupportedFlavorException | IOException e) {
				e.printStackTrace();
			}
            return false;
		}
		
		public int getSourceActions(final JComponent c) {
		    return COPY;
		}
		
	}
	
}