package btdex.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import btdex.core.ContractState;
import btdex.core.Globals;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.http.BRSError;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class TransactionsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon;

	public static final int COL_ID = 0;
	public static final int COL_TIME = 1;
	public static final int COL_TYPE = 2;
	public static final int COL_AMOUNT = 3;
	public static final int COL_FEE = 4;
	public static final int COL_ACCOUNT = 5;
	public static final int COL_CONF = 6;

	String[] columnNames = {
			"TRANSACTION ID",
			"TIME",
			"TYPE",
			"AMOUNT (BURST)",
			"FEE (BURST)",
			"ACCOUNT",
			"CONF.",
	};

	class MyTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int col) {
			String colName = columnNames[col];
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_ID || col == COL_ACCOUNT;
		}
	}

	public TransactionsPanel() {
		super(new BorderLayout());

		table = new JTable(model = new MyTableModel());
		table.setRowHeight(table.getRowHeight()+7);
		
		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());

		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);

		// Center header and all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );			
		}
		JTableHeader jtableHeader = table.getTableHeader();
		DefaultTableCellRenderer rend = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);

		table.setAutoCreateColumnsFromModel(false);

		table.getColumnModel().getColumn(COL_ID).setCellRenderer(OrderBook.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_ID).setCellEditor(OrderBook.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_ACCOUNT).setCellRenderer(OrderBook.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_ACCOUNT).setCellEditor(OrderBook.BUTTON_EDITOR);
		//
		table.getColumnModel().getColumn(COL_ACCOUNT).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_ID).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_TIME).setPreferredWidth(120);

		add(scrollPane, BorderLayout.CENTER);
	}

	public void update() {
		Globals g = Globals.getInstance();
		
		ArrayList<Transaction> txs = new ArrayList<>();

		try {
			Transaction[] unconf = g.getNS().getUnconfirmedTransactions(g.getAddress()).blockingGet();
			Transaction[] conf = g.getNS().getAccountTransactions(g.getAddress()).blockingGet();
			
			for (int i = 0; i < unconf.length; i++) {
				txs.add(unconf[i]);
			}
			for (int i = 0; i < conf.length; i++) {
				txs.add(conf[i]);
			}
		}
		catch (Exception e) {
			if(e.getCause() instanceof BRSError) {
				BRSError error = (BRSError) e.getCause();
				if(error.getCode() != 5) // unknown account
					throw e;
			}
			else
				throw e;
			// Unknown account, no transactions
		}

		int maxLines = Math.min(txs.size(), 200);

		model.setRowCount(maxLines);

		// Update the contents
		for (int row = 0; row < maxLines; row++) {
			Transaction tx = txs.get(row);
			BurstAddress account = null;
			long amount = tx.getAmount().longValue();

			if(tx.getSender() == g.getAddress())
				amount = -amount;

			String type = "Payment";
			switch (tx.getType()) {
			case 2:
				switch (tx.getSubtype()) {
				case 0:
					type = "Token Issuance";
					break;
				case 1:
					type = "Token Transfer";
					break;
				case 2:
					type = "Ask Offer";
					break;
				case 3:
					type = "Bid Offer";
					break;
				case 4:
					type = "Cancel Ask";
					break;
				case 5:
					type = "Cancel Bid";
					break;
				default:
					break;
				}
				break;
			case 22:
				switch (tx.getSubtype()) {
				case 0:
					type = "SC Creation";
					break;
				case 1:
					type = "SC Payment";
				default:
					break;
				}
				break;
			default:
				break;
			}

			if(tx.getSender()!=null && tx.getSender().getSignedLongId() != g.getAddress().getSignedLongId())
				account = tx.getSender();
			if(tx.getRecipient()!=null && tx.getRecipient().getSignedLongId()!= g.getAddress().getSignedLongId())
				account = tx.getRecipient();

			model.setValueAt(tx.getBlockId()==null ? "PENDING" : tx.getConfirmations(), row, COL_CONF);
			model.setValueAt(account==null ? new JLabel() :
				new CopyToClipboardButton(account.getRawAddress(), copyIcon, account.getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);
			model.setValueAt(new CopyToClipboardButton(tx.getId().toString(), copyIcon, OrderBook.BUTTON_EDITOR), row, COL_ID);

			model.setValueAt(ContractState.format(amount), row, COL_AMOUNT);
			model.setValueAt(ContractState.format(tx.getFee().longValue()), row, COL_FEE);
			model.setValueAt(type, row, COL_TYPE);
			model.setValueAt(HistoryPanel.DATE_FORMAT.format(tx.getTimestamp().getAsDate()), row, COL_TIME);
		}
	}
}
