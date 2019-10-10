package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;

import btdex.core.ContractState;
import btdex.core.Globals;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Transaction;

public class TransactionsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;

	public static final int COL_ID = 0;
	public static final int COL_DATE = 1;
	public static final int COL_TYPE = 2;
	public static final int COL_AMOUNT = 3;
	public static final int COL_FEE = 4;
	public static final int COL_ACCOUNT = 5;
	public static final int COL_CONF = 6;

	String[] columnNames = {
			"TRANSACTION ID",
			"DATE",
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
			return col == COL_ID;
		}
	}

	ButtonCellRenderer RENDERER = new ButtonCellRenderer();
	public class ButtonCellRenderer extends DefaultTableCellRenderer	{
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			return (JButton)value;
		}
	}

	ButtonCellEditor EDITOR = new ButtonCellEditor();
	class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;

		JButton but;

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {

			return but = (JButton) value;
		}

		public Object getCellEditorValue() {
			return but;
		}

		// validate the input
		public boolean stopCellEditing() {
			return super.stopCellEditing();
		}
	};

	public TransactionsPanel() {
		super(new BorderLayout());

		table = new JTable(model = new MyTableModel());

		table.setRowHeight(table.getRowHeight()+7);

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

		table.getColumnModel().getColumn(COL_ID).setCellRenderer(RENDERER);
		table.getColumnModel().getColumn(COL_ID).setCellEditor(EDITOR);
		//
		table.getColumnModel().getColumn(COL_ACCOUNT).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_ID).setPreferredWidth(200);

		add(scrollPane, BorderLayout.CENTER);
	}

	public void update() {
		Globals g = Globals.getInstance();

		Transaction[] txs = g.getNS().getAccountTransactions(g.getAddress()).blockingGet();

		int maxLines = Math.min(txs.length, 200);

		model.setRowCount(maxLines);

		// Update the contents
		for (int row = 0; row < maxLines; row++) {
			Transaction tx = txs[row];
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

			model.setValueAt(tx.getConfirmations(), row, COL_CONF);
			model.setValueAt(account==null ? "" : account.getRawAddress(), row, COL_ACCOUNT);
			model.setValueAt(new JButton(tx.getId().toString()), row, COL_ID);

			model.setValueAt(ContractState.format(amount), row, COL_AMOUNT);
			model.setValueAt(ContractState.format(tx.getFee().longValue()), row, COL_FEE);
			model.setValueAt(type, row, COL_TYPE);
			model.setValueAt(tx.getTimestamp().toString(), row, COL_DATE);
		}
	}
}
