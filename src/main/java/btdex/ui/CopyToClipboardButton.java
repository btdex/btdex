package btdex.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

public class CopyToClipboardButton extends JButton {
	private static final long serialVersionUID = 1L;
	private String clipboard;

	public CopyToClipboardButton(String text, Icon icon) {
		this(text, icon, text, null);
	}
	
	public CopyToClipboardButton(String text, Icon icon, TableCellEditor editor) {
		this(text, icon, text, editor);
	}

	public CopyToClipboardButton(String text, Icon icon, String clipboardText, TableCellEditor editor) {
		this(text, icon, clipboardText, editor, null);
	}
	
	public CopyToClipboardButton(String text, Icon icon, String clipboardText, TableCellEditor editor,
			String tooltipText) {
		super(text, icon);
		this.clipboard = clipboardText;
		if(tooltipText!=null)
			setToolTipText(tooltipText);
		
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection stringSelection = new StringSelection(CopyToClipboardButton.this.clipboard);
				clipboard.setContents(stringSelection, null);
				
				JFrame f = (JFrame) SwingUtilities.getRoot(CopyToClipboardButton.this);
				Toast.makeText(f, CopyToClipboardButton.this.clipboard + " copied to clipboard.",
						Toast.Style.SUCCESS).display(CopyToClipboardButton.this);
				
				if(editor!=null)
					editor.stopCellEditing();
			}
		});
	}
	
	public void setClipboard(String txt) {
		this.clipboard = txt;
	}	
}
