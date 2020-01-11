package btdex.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.table.TableCellEditor;

public class ExplorerButton extends JButton {
	private static final long serialVersionUID = 1L;
	private String id, addressRS;
	private int type = TYPE_ADDRESS;
	
	public static final int TYPE_ADDRESS = 0;
	public static final int TYPE_TRANSACTION = 1;

	public ExplorerButton(String text, Icon icon) {
		this(text, icon, null);
	}
	
	public ExplorerButton(String text, Icon icon, TableCellEditor editor) {
		this(text, icon, TYPE_TRANSACTION, text, editor);
	}

	public ExplorerButton(String text, Icon icon, int type, String id, TableCellEditor editor) {
		this(text, icon, type, id, null, editor, null);
	}

	public ExplorerButton(String text, Icon icon, int type, String id, String addressRS, TableCellEditor editor) {
		this(text, icon, type, id, addressRS, editor, null);
	}
	
	public ExplorerButton(String text, Icon icon, int type, String id, String addressRS, TableCellEditor editor,
			String tooltipText) {
		super(text, icon);
		this.type = type;
		this.id = id;
		this.addressRS = addressRS;
		if(tooltipText!=null)
			setToolTipText(tooltipText);
		
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OpenExplorer exp = Main.getInstance().explorer;
				
				switch (ExplorerButton.this.type) {
				case TYPE_ADDRESS:
					exp.openAddress(ExplorerButton.this.addressRS, ExplorerButton.this.id);
					break;
				case TYPE_TRANSACTION:
					exp.openTransaction(ExplorerButton.this.id);
					break;
				default:
					break;
				}
				if(editor!=null)
					editor.stopCellEditing();
			}
		});
	}
	
	public void setAddress(String id, String addressRS) {
		this.type = TYPE_ADDRESS;
		this.id = id;
		this.addressRS = addressRS;
	}	
}
