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

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.attachment.AskOrderPlacementAttachment;
import burst.kit.entity.response.attachment.AssetTransferAttachment;
import burst.kit.entity.response.attachment.BidOrderPlacementAttachment;
import burst.kit.entity.response.http.BRSError;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class TransactionsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon, expIcon;

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
			"AMOUNT",
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
		table.setRowHeight(table.getRowHeight()+10);
		table.setRowSelectionAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);
		
		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());
		expIcon = IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, 12, table.getForeground());

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
		BurstNode bn = BurstNode.getInstance();
		
		ArrayList<Transaction> txs = new ArrayList<>();

		try {
			// Get all unconf. txs, not only for this account, this way we can catch the
			// activation message for new accounts.
			Transaction[] unconf = bn.getUnconfirmedTransactions();
			for (int i = 0; unconf !=null && i < unconf.length; i++) {
				Transaction utx = unconf[i];
				if((utx.getRecipient()!=null && utx.getRecipient().getSignedLongId() == g.getAddress().getSignedLongId()) ||
						(utx.getSender()!=null && utx.getSender().getSignedLongId() == g.getAddress().getSignedLongId()) )
					txs.add(utx);
			}
			
			Transaction[] conf = bn.getAccountTransactions();
			for (int i = 0; conf != null && i < conf.length; i++) {
				txs.add(conf[i]);
			}
		}
		catch (Exception e) {
			if(e.getCause() instanceof BRSError) {
				BRSError error = (BRSError) e.getCause();
				if(error.getCode() != 5) // unknown account, we don't need to print this
					e.printStackTrace();
			}
			else
				e.printStackTrace();
		}

		int maxLines = Math.min(txs.size(), 200);

		model.setRowCount(maxLines);

		// Update the contents
		for (int row = 0; row < maxLines; row++) {
			Transaction tx = txs.get(row);
			BurstAddress account = null;
			long amount = tx.getAmount().longValue();

			String amountFormatted = NumberFormatting.BURST.format(amount) + " " + Constants.BURST_TICKER;
			if(tx.getSender().getSignedLongId() == g.getAddress().getSignedLongId() && amount > 0L)
				amountFormatted = "- " + amountFormatted;

			// Types defined at brs/TransactionType.java
			String type = "Payment";
			switch (tx.getType()) {
			case 1: // TYPE_PAYMENT
				switch (tx.getSubtype()) {
				case 1:
					type = "Alias assingment";
					break;
				case 5:
					type = "Account info";
					break;
				case 6:
					type = "Alias sell";
					break;
				case 7:
					type = "Alias buy";
					break;
				default:
					type = "Message";
					amountFormatted = "";
				}
				break;
			case 2: // TYPE_MESSAGING
				switch (tx.getSubtype()) {
				case 0:
					type = "Token Issuance";
					break;
				case 1:
					type = "Token Transfer";
					if(tx.getAttachment() instanceof AssetTransferAttachment) {
						AssetTransferAttachment assetTx = (AssetTransferAttachment) tx.getAttachment();
						for(Market market : Markets.getMarkets()) {
							if(market.getTokenID()!=null && market.getTokenID().getID().equals(assetTx.getAsset())) {
								long tokenAmount = Long.parseLong(assetTx.getQuantityQNT());
								amountFormatted = market.format(tokenAmount) + " " + market.toString();
								if(tx.getSender().getSignedLongId() == g.getAddress().getSignedLongId())
									amountFormatted = "- " + amountFormatted;
								break;
							}
						}
					}
					break;
				case 2:
					type = "Ask Offer";
					if(tx.getAttachment() instanceof AskOrderPlacementAttachment) {
						AskOrderPlacementAttachment order = (AskOrderPlacementAttachment) tx.getAttachment();
						for(Market market : Markets.getMarkets()) {
							if(market.getTokenID()!=null && market.getTokenID().getID().equals(order.getAsset())) {
								long tokenAmount = Long.parseLong(order.getQuantityQNT());
								amountFormatted = market.format(tokenAmount) + " " + market.toString();
								break;
							}
						}
					}
					break;
				case 3:
					type = "Bid Offer";
					if(tx.getAttachment() instanceof BidOrderPlacementAttachment) {
						BidOrderPlacementAttachment order = (BidOrderPlacementAttachment) tx.getAttachment();
						for(Market market : Markets.getMarkets()) {
							if(market.getTokenID()!=null && market.getTokenID().getID().equals(order.getAsset())) {
								long tokenAmount = Long.parseLong(order.getQuantityQNT());
								amountFormatted = market.format(tokenAmount) + " " + market.toString();
								break;
							}
						}
					}
					break;
				case 4:
					type = "Cancel Ask";
					amountFormatted = "";
					break;
				case 5:
					type = "Cancel Bid";
					amountFormatted = "";
					break;
				default:
					break;
				}
				break;
			case 20: // TYPE_MINING
				type = "Set reward rec.";
				break;
			case 22: // TYPE_AUTOMATED_TRANSACTIONS
				switch (tx.getSubtype()) {
				case 0:
					type = "SC Creation";
					amountFormatted = "";
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
				new ExplorerButton(account.getRawAddress(), copyIcon, expIcon, ExplorerButton.TYPE_ADDRESS,
						account.getID(), account.getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);
			model.setValueAt(new ExplorerButton(tx.getId().toString(), copyIcon, expIcon, OrderBook.BUTTON_EDITOR), row, COL_ID);

			model.setValueAt(amountFormatted, row, COL_AMOUNT);
			model.setValueAt(tx.getFee().toUnformattedString(), row, COL_FEE);
			model.setValueAt(type, row, COL_TYPE);
			model.setValueAt(HistoryPanel.DATE_FORMAT.format(tx.getTimestamp().getAsDate()), row, COL_TIME);
		}
	}
}
