package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import btdex.ui.orderbook.BookTable;

public class ExplorerButton extends JPanel {
	private static final long serialVersionUID = 1L;
	private String id, addressRS;
	private int type = TYPE_ADDRESS;
	private boolean isBinance = false;
	
	public static final int TYPE_ADDRESS = 0;
	public static final int TYPE_TRANSACTION = 1;
	public static final int TYPE_TOKEN = 2;
	
	JButton mainButton;
	JButton explorerButton;
	
	public ExplorerButton(String text, Icon icon, Icon icon2) {
		this(text, icon, icon2, TYPE_TRANSACTION, text);
	}

	public ExplorerButton(String text, Icon icon, Icon icon2, int type, String id) {
		this(text, icon, icon2, type, id, null);
	}

	public ExplorerButton(String text, Icon icon, Icon icon2, int type, String id, String addressRS) {
		this(text, icon, icon2, type, id, addressRS, null);
	}
	
	public void setBinance(boolean b) {
		this.isBinance = b;
	}
	
	public String getId() {
		return id;
	}
	
	public ExplorerButton(String text, Icon icon, Icon icon2, int type, String id, String addressRS,
			String tooltipText) {
		super(new BorderLayout(0, 0));
		
		mainButton = new JButton(text, icon);
		mainButton.setToolTipText(tr("btn_copy_to_clipboard"));
		explorerButton = new JButton(icon2);
		explorerButton.setToolTipText(tr("btn_open_on_explorer"));
		
		this.type = type;
		this.id = id;
		this.addressRS = addressRS;
		if(tooltipText!=null)
			mainButton.setToolTipText(tooltipText);

		mainButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String t = ExplorerButton.this.type == TYPE_ADDRESS ? ExplorerButton.this.addressRS : ExplorerButton.this.id;
				if(t == null || t.length() == 0)
					return;
				
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection stringSelection = new StringSelection(t);
				clipboard.setContents(stringSelection, null);
				
				Toast.makeText(Main.getInstance(), tr("btn_copied_to_clipboard", t), Toast.Style.SUCCESS).display();
			}
		});
		
		explorerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(ExplorerButton.this.id == null || ExplorerButton.this.id.length() == 0)
					return;
				
				ExplorerWrapper exp = Main.getInstance().getExplorer();
				if(isBinance)
					exp = ExplorerWrapper.binanceExplorer();
				
				switch (ExplorerButton.this.type) {
				case TYPE_ADDRESS:
					Main.getInstance().browse(exp.openAddress(ExplorerButton.this.addressRS, ExplorerButton.this.id));
					break;
				case TYPE_TRANSACTION:
					Main.getInstance().browse(exp.openTransaction(ExplorerButton.this.id));
					break;
				case TYPE_TOKEN:
					Main.getInstance().browse(exp.openToken(ExplorerButton.this.id));
					break;
				default:
					break;
				}
				BookTable.BUTTON_EDITOR.stopCellEditing();
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
	
	
	public void setTokenID(String id) {
		this.type = TYPE_TOKEN;
		this.id = id;
		this.addressRS = null;
	}
}
