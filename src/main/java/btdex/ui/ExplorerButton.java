package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.TableCellEditor;

public class ExplorerButton extends JPanel {
	private static final long serialVersionUID = 1L;
	private String id, addressRS;
	private int type = TYPE_ADDRESS;
	
	public static final int TYPE_ADDRESS = 0;
	public static final int TYPE_TRANSACTION = 1;
	
	JButton mainButton;
	JButton explorerButton;

	public ExplorerButton(String text, Icon icon, Icon icon2) {
		this(text, icon, icon2, null);
	}
	
	public ExplorerButton(String text, Icon icon, Icon icon2, TableCellEditor editor) {
		this(text, icon, icon2, TYPE_TRANSACTION, text, editor);
	}

	public ExplorerButton(String text, Icon icon, Icon icon2, int type, String id, TableCellEditor editor) {
		this(text, icon, icon2, type, id, null, editor, null);
	}

	public ExplorerButton(String text, Icon icon, Icon icon2, int type, String id, String addressRS, TableCellEditor editor) {
		this(text, icon, icon2, type, id, addressRS, editor, null);
	}
	
	public ExplorerButton(String text, Icon icon, Icon icon2, int type, String id, String addressRS, TableCellEditor editor,
			String tooltipText) {
		super(new BorderLayout(0, 0));
		
		mainButton = new JButton(text, icon);
		mainButton.setToolTipText("Copy to clipboard");
		explorerButton = new JButton(icon2);
		explorerButton.setToolTipText("Open on the explorer");
		
		this.type = type;
		this.id = id;
		this.addressRS = addressRS;
		if(tooltipText!=null)
			mainButton.setToolTipText(tooltipText);

		mainButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String t = ExplorerButton.this.type == TYPE_ADDRESS ? ExplorerButton.this.addressRS : ExplorerButton.this.id;
				
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection stringSelection = new StringSelection(t);
				clipboard.setContents(stringSelection, null);
				
				Toast.makeText(Main.getInstance(), t + " copied to clipboard.", Toast.Style.SUCCESS).display();
			}
		});
		
		explorerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ExplorerWrapper exp = Main.getInstance().explorer;
				
				switch (ExplorerButton.this.type) {
				case TYPE_ADDRESS:
					Main.getInstance().browse(exp.openAddress(ExplorerButton.this.addressRS, ExplorerButton.this.id));
					break;
				case TYPE_TRANSACTION:
					Main.getInstance().browse(exp.openTransaction(ExplorerButton.this.id));
					break;
				default:
					break;
				}
				if(editor!=null)
					editor.stopCellEditing();
			}
		});
				
		add(mainButton, BorderLayout.CENTER);
		add(explorerButton, BorderLayout.EAST);
	}
	
	public JButton getMainButton() {
		return mainButton;
	}
	
	public JButton getExplorerButton() {
		return explorerButton;
	}
	
	public void setAddress(String id, String addressRS) {
		this.type = TYPE_ADDRESS;
		this.id = id;
		this.addressRS = addressRS;
	}	
}
