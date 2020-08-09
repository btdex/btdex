package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
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
import btdex.ui.orderbook.BookTable;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Block;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.attachment.AskOrderPlacementAttachment;
import burst.kit.entity.response.attachment.AssetTransferAttachment;
import burst.kit.entity.response.attachment.BidOrderPlacementAttachment;
import burst.kit.entity.response.attachment.MultiOutAttachment;
import burst.kit.entity.response.attachment.MultiOutSameAttachment;
import burst.kit.entity.response.http.BRSError;
import jiconfont.swing.IconFontSwing;

public class TransactionsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	JLabel statusLabel;
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
			"txs_id",
			"hist_time",
			"txs_type",
			"hist_amount",
			"txs_fee",
			"txs_account",
			"txs_conf",
	};

	class MyTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int col) {
			String colName = tr(columnNames[col]);
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
		
		copyIcon = IconFontSwing.buildIcon(Icons.COPY, 12, table.getForeground());
		expIcon = IconFontSwing.buildIcon(Icons.EXPLORER, 12, table.getForeground());

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

		table.getColumnModel().getColumn(COL_ID).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_ID).setCellEditor(BookTable.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_ACCOUNT).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_ACCOUNT).setCellEditor(BookTable.BUTTON_EDITOR);
		//
		table.getColumnModel().getColumn(COL_ACCOUNT).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_ID).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_TIME).setPreferredWidth(120);
		
		statusLabel = new JLabel(" ", JLabel.RIGHT);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		add(statusLabel, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void update() {
		Globals g = Globals.getInstance();
		BurstNode bn = BurstNode.getInstance();
		
		ArrayList<Transaction> txs = new ArrayList<>();
		
		Block latest = bn.getLatestBlock();
		Date now = new Date();
		if(latest != null) {
			int mins = 4 - (int) ((now.getTime() - latest.getTimestamp().getAsDate().getTime())/1000 / 60);
			statusLabel.setText(mins > 1 ? tr("txs_next_block", mins) : tr("txs_next_block_late"));
		}

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
			String type = tr("txs_payment");
			switch (tx.getType()) {
			case 0: // PAYMENT
				if(!tx.getSender().equals(g.getAddress())) {
					if(tx.getSender().getBurstID().getSignedLongId() == Constants.TRT_DIVIDENDS)
						type = tr("txs_fees_distribution");
					switch (tx.getSubtype()) {
					case 1: // MULTI-OUT
						if(tx.getAttachment() instanceof MultiOutAttachment) {
							MultiOutAttachment attach = (MultiOutAttachment) tx.getAttachment();
							amountFormatted = NumberFormatting.BURST.format(
									attach.getOutputs().get(g.getAddress()).longValue()) + " " + Constants.BURST_TICKER;
						}
						break;
					case 2: // MULTI-SAME
						if(tx.getAttachment() instanceof MultiOutSameAttachment) {
							MultiOutSameAttachment attach = (MultiOutSameAttachment) tx.getAttachment();
							amountFormatted = NumberFormatting.BURST.format(amount/attach.getRecipients().length) + " " + Constants.BURST_TICKER;
						}						
						break;
					}
				}
				break;
			case 1: // TYPE_PAYMENT
				switch (tx.getSubtype()) {
				case 1:
					type = tr("txs_alias_assign");
					break;
				case 5:
					type = tr("txs_account_info");
					break;
				case 6:
					type = tr("txs_alias_sell");
					break;
				case 7:
					type = tr("txs_alias_buy");
					break;
				default:
					type = tr("txs_message");
					amountFormatted = "";
				}
				break;
			case 2: // TYPE_MESSAGING
				switch (tx.getSubtype()) {
				case 0:
					type = tr("txs_token_issue");
					break;
				case 1:
					type = tr("txs_token_transfer");
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
					type = tr("txs_ask_offer");
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
					type = tr("txs_bid_offer");
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
					type = tr("txs_cancel_ask");
					amountFormatted = "";
					break;
				case 5:
					type = tr("txs_cancel_bid");
					amountFormatted = "";
					break;
				default:
					break;
				}
				break;
			case 20: // TYPE_MINING
				type = tr("txs_set_reward");
				break;
			case 22: // TYPE_AUTOMATED_TRANSACTIONS
				switch (tx.getSubtype()) {
				case 0:
					type = tr("txs_sc_create");
					amountFormatted = "";
					break;
				case 1:
					type = tr("txs_sc_payment");
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

			model.setValueAt(tx.getBlockId()==null ? tr("book_pending_button") : tx.getConfirmations(), row, COL_CONF);
			model.setValueAt(account==null ? new JLabel() :
				new ExplorerButton(account.getRawAddress(), copyIcon, expIcon, ExplorerButton.TYPE_ADDRESS,
						account.getID(), account.getFullAddress()), row, COL_ACCOUNT);
			model.setValueAt(new ExplorerButton(tx.getId().toString(), copyIcon, expIcon), row, COL_ID);

			model.setValueAt(amountFormatted, row, COL_AMOUNT);
			model.setValueAt(tx.getFee().toUnformattedString(), row, COL_FEE);
			model.setValueAt(type, row, COL_TYPE);
			model.setValueAt(HistoryPanel.DATE_FORMAT.format(tx.getTimestamp().getAsDate()), row, COL_TIME);
		}
	}
}
