package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import bt.Contract;
import btdex.core.ContractState;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import static btdex.locale.Translation.tr;
import btdex.sc.SellContract;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.AssetTrade;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class OrderBook extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon, expIcon, upIcon, downIcon, cancelIcon, editIcon, takeIcon, withdrawIcon;
	JCheckBox listOnlyMine;
	JLabel lastPrice;
	JButton buyButton, sellButton;
	AssetOrder firstBid, firstAsk;

	int ROW_HEIGHT;

	ArrayList<ContractState> contracts = new ArrayList<>();
	ArrayList<ContractState> contractsBuy = new ArrayList<>();

	public static final int COL_CONTRACT = 0;
	public static final int COL_TOTAL = 1;
	public static final int COL_SIZE = 2;
	public static final int COL_PRICE = 3;

	public static final int[] BID_COLS = {0, 1, 2, 3};
	public static final int[] ASK_COLS = {7, 6, 5, 4};

	private static final int COL_WIDE = 100;
	private static final int COL_REGULAR = 75;

	String[] columnNames = {
			"book_contract",
			"book_total",
			"book_size",
			"book_price",

			"book_price",
			"book_size",
			"book_total",
			"book_contract",
	};

	Market market = null, newMarket;

	public static final ButtonCellRenderer BUTTON_RENDERER = new ButtonCellRenderer();
	public static final ButtonCellEditor BUTTON_EDITOR = new ButtonCellEditor();

	class MyTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int col) {
			boolean isToken = market.getTokenID()!=null;

			String colName = columnNames[col];
			if(col == BID_COLS[COL_PRICE] || col == ASK_COLS[COL_PRICE])
				colName = tr("book_price", isToken ? "BURST" : market);
			else if(col == BID_COLS[COL_TOTAL] || col == ASK_COLS[COL_TOTAL])
				colName = tr("book_total", isToken ? "BURST" : market);
			else if(col == BID_COLS[COL_SIZE] || col == ASK_COLS[COL_SIZE]) {
				if(isToken)
					colName = tr("book_size", market);
				else
					colName = tr("book_size", "BURST") + " (" + tr("book_deposit") + ")";
			}
			else if((col == BID_COLS[COL_CONTRACT] || col==ASK_COLS[COL_CONTRACT]) && isToken)
				colName = tr("book_order");
			else
				colName = tr(colName);
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == BID_COLS[COL_CONTRACT] || col == ASK_COLS[COL_CONTRACT]
					|| col == BID_COLS[COL_PRICE] || col == ASK_COLS[COL_PRICE];
		}
	}

	class MyTable extends JTable {
		private static final long serialVersionUID = 3251005544025726619L;

		public MyTable(DefaultTableModel model) {
			super(model);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int col) {
			if(col == BID_COLS[COL_CONTRACT] || col == ASK_COLS[COL_CONTRACT]
					|| col == BID_COLS[COL_PRICE] || col == ASK_COLS[COL_PRICE])
				return BUTTON_RENDERER;

			return super.getCellRenderer(row, col);
		}

		@Override
		public TableCellEditor getCellEditor(int row, int col) {
			if(col == BID_COLS[COL_CONTRACT] || col == ASK_COLS[COL_CONTRACT]
					|| col == BID_COLS[COL_PRICE] || col == ASK_COLS[COL_PRICE])
				return BUTTON_EDITOR;

			return super.getCellEditor(row, col);
		}


	}

	public static class ButtonCellRenderer extends DefaultTableCellRenderer	{
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			return (Component)value;
		}
	}


	public static class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;

		Component but;

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {

			return but = (Component) value;
		}

		public Object getCellEditorValue() {
			return but;
		}

		// validate the input
		public boolean stopCellEditing() {
			return super.stopCellEditing();
		}
	};

	public class ActionButton extends JButton{
		private static final long serialVersionUID = 1L;

		AssetOrder order;
		ContractState contract;
		boolean isToken;
		boolean cancel;

		public ActionButton(String text, ContractState contract, boolean cancel) {
			this(text, null, contract, cancel, false);
		}

		public ActionButton(String text, AssetOrder order, boolean cancel) {
			this(text, order, null, cancel, true);
		}

		public ActionButton(String text, AssetOrder order, ContractState contract, boolean cancel, boolean isToken) {
			super(text);
			this.order = order;
			this.contract = contract;
			this.cancel = cancel;
			this.isToken = isToken;

			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);

					JDialog dlg = null;
					if(cancel) {
						dlg = new CancelOrderDialog(f, market, order, contract);
					}
					else {
						if(isToken)
							dlg = new PlaceTokenOrderDialog(f, market, order);
						else
							dlg = new PlaceOrderDialog(f, market, contract, false);
					}
					dlg.setLocationRelativeTo(OrderBook.this);
					dlg.setVisible(true);

					BUTTON_EDITOR.stopCellEditing();
				}
			});
		}
	}

	public void setMarket(Market m) {
		newMarket = m;		
	}

	public OrderBook(Main main, Market m) {
		super(new BorderLayout());

		listOnlyMine = new JCheckBox(tr("book_mine_only"));
		listOnlyMine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.setRowCount(0);
				model.fireTableDataChanged();
				main.update();
			}
		});
		
		market = m;

		table = new MyTable(model = new MyTableModel());
		ROW_HEIGHT = table.getRowHeight()+10;
		table.setRowHeight(ROW_HEIGHT);
		table.setRowSelectionAllowed(false);

		// Allowing to hide columns
		for (int i = 0; i < columnNames.length; i++) {
			table.getColumnModel().getColumn(i).setMinWidth(0);			
		}

		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());
		expIcon = IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, 12, table.getForeground());
		upIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_UP, 18, HistoryPanel.GREEN);
		downIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_DOWN, 18, HistoryPanel.RED);
		cancelIcon = IconFontSwing.buildIcon(FontAwesome.TIMES, 12, table.getForeground());
		editIcon = IconFontSwing.buildIcon(FontAwesome.PENCIL, 12, table.getForeground());
		takeIcon = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 12, table.getForeground());
		withdrawIcon = IconFontSwing.buildIcon(FontAwesome.RECYCLE, 12, table.getForeground());

		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);

		// Center header and all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );
			table.getColumnModel().getColumn(i).setPreferredWidth(COL_REGULAR);
		}
		JTableHeader jtableHeader = table.getTableHeader();
		DefaultTableCellRenderer rend = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);

		table.setAutoCreateColumnsFromModel(false);

		table.getColumnModel().getColumn(BID_COLS[COL_CONTRACT]).setPreferredWidth(COL_WIDE);
		table.getColumnModel().getColumn(ASK_COLS[COL_CONTRACT]).setPreferredWidth(COL_WIDE);

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new Desc(tr("book_last_price"), lastPrice = new JLabel()));

		top.add(new Desc(" ", buyButton = new JButton()));
		top.add(new Desc(" ", sellButton = new JButton()));

		top.add(new Desc(" ", listOnlyMine));

		buyButton.setBackground(HistoryPanel.GREEN);
		sellButton.setBackground(HistoryPanel.RED);
		buyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);
				JDialog dlg = market.getTokenID()!= null ? new PlaceTokenOrderDialog(f, market, firstAsk):
					new PlaceOrderDialog(f, market, null, true);
				dlg.setLocationRelativeTo(OrderBook.this);
				dlg.setVisible(true);
			}
		});
		sellButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);
				JDialog dlg = market.getTokenID()!= null ? new PlaceTokenOrderDialog(f, market, firstBid)
						: new PlaceOrderDialog(f, market, null, false);
				
				dlg.setLocationRelativeTo(OrderBook.this);
				dlg.setVisible(true);
			}
		});

		add(top, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);

		market = null;
		setMarket(m);
	}

	public void update() {
		if(newMarket != market) {
			market = newMarket;
			
			model.setRowCount(0);
			model.fireTableDataChanged();
			
			if(market.getTokenID() != null) {
				buyButton.setText(tr("book_buy_button", market));
				sellButton.setText(tr("book_sell_button", market));
			}
			else {
				buyButton.setText(tr("book_buy_button", "BURST"));
				sellButton.setText(tr("book_sell_button", "BURST"));
			}
			lastPrice.setIcon(null);
			lastPrice.setText(" ");

			// update the column headers
			for (int c = 0; c < columnNames.length; c++) {
				table.getColumnModel().getColumn(c).setHeaderValue(model.getColumnName(c));
			}
			table.getTableHeader().repaint();
		}

		if(market.getTokenID()==null) {
			updateContracts();
		}
		else
			updateOrders();
	}

	private void updateOrders() {
		BurstID token = market.getTokenID();

		Globals g = Globals.getInstance();

		boolean onlyMine = listOnlyMine.isSelected();

		ArrayList<AssetOrder> bidOrders = new ArrayList<>();
		ArrayList<AssetOrder> askOrders = new ArrayList<>();
		AssetOrder[] bids = g.getNS().getBidOrders(token).blockingGet();
		AssetOrder[] asks = g.getNS().getAskOrders(token).blockingGet();

		for (int i = 0; i < asks.length; i++) {
			if(onlyMine && asks[i].getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
				continue;
			askOrders.add(asks[i]);
		}
		for (int i = 0; i < bids.length; i++) {
			if(onlyMine && bids[i].getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
				continue;
			bidOrders.add(bids[i]);
		}

		// sort by price
		bidOrders.sort(new Comparator<AssetOrder>() {
			@Override
			public int compare(AssetOrder o1, AssetOrder o2) {
				int cmp = (int)(o2.getPrice().longValue() - o1.getPrice().longValue());
				if(cmp == 0)
					cmp = o2.getHeight() - o1.getHeight();
				return cmp;
			}
		});
		// sort by price
		askOrders.sort(new Comparator<AssetOrder>() {
			@Override
			public int compare(AssetOrder o1, AssetOrder o2) {
				int cmp = -(int)(o2.getPrice().longValue() - o1.getPrice().longValue());
				if(cmp == 0)
					cmp = o2.getHeight() - o1.getHeight();
				return cmp;
			}
		});

		firstBid = bidOrders.size() > 0 ? bidOrders.get(0) : null;
		firstAsk = askOrders.size() > 0 ? askOrders.get(0) : null;

		model.setRowCount(Math.max(bids.length, asks.length));

		AssetTrade trs[] = g.getNS().getAssetTrades(market.getTokenID(), null, 0, 1).blockingGet();
		AssetTrade lastTrade = trs.length > 0 ? trs[0] : null;
		boolean lastIsUp = true;
		if(trs.length > 1 && trs[0].getPrice().longValue() < trs[1].getPrice().longValue())
			lastIsUp = false;

		if(lastTrade != null) {
			// set the last price label
			String priceLabel = NumberFormatting.BURST.format(lastTrade.getPrice().longValue()*market.getFactor()) + " BURST";
			lastPrice.setText(priceLabel);
			lastPrice.setIcon(lastIsUp ? upIcon : downIcon);
			lastPrice.setForeground(lastIsUp ? HistoryPanel.GREEN : HistoryPanel.RED);
		}

		addOrders(bidOrders, BID_COLS);
		addOrders(askOrders, ASK_COLS);
	}

	private void addOrders(ArrayList<AssetOrder> orders, int[] cols) {
		int row = 0;
		for (; row < orders.size(); row++) {
			AssetOrder o = orders.get(row);

			// price always come in Burst, so no problem in this division using long's
			long price = o.getPrice().longValue();
			long amountToken = o.getQuantity().longValue();

			if(price == 0 || amountToken == 0)
				continue;

			String priceFormated = NumberFormatting.BURST.format(price*market.getFactor());
			JButton b = new ActionButton(priceFormated, o, false);
			if(o.getAccountAddress().getSignedLongId() == Globals.getInstance().getAddress().getSignedLongId()) {
				b = new ActionButton(priceFormated, o, true);
				b.setIcon(cancelIcon);
			}
			b.setBackground(o.getType() == AssetOrder.OrderType.ASK ? HistoryPanel.RED : HistoryPanel.GREEN);
			model.setValueAt(b, row, cols[COL_PRICE]);

			model.setValueAt(market.format(amountToken), row, cols[COL_SIZE]);
			model.setValueAt(NumberFormatting.BURST.format(amountToken*price), row, cols[COL_TOTAL]);

			ExplorerButton exp = new ExplorerButton(o.getId().getID(), copyIcon, expIcon, BUTTON_EDITOR);
			if(o.getAccountAddress().getSignedLongId() == Globals.getInstance().getAddress().getSignedLongId()) {
				JButton cancel = new ActionButton("", o, true);
				cancel.setIcon(cancelIcon);
				exp.add(cancel, BorderLayout.WEST);
			}
			model.setValueAt(exp, row, cols[COL_CONTRACT]);
		}
		// fill with null all the remaining rows
		for (; row < model.getRowCount(); row++) {
			for (int col = 0; col < cols.length; col++) {
				model.setValueAt(null, row, cols[col]);
			}
		}
	}

	private void updateContracts() {
		Globals g = Globals.getInstance();

		Collection<ContractState> allContracts = Contracts.getContracts();
		contracts.clear();
		contractsBuy.clear();
		boolean onlyMine = listOnlyMine.isSelected();

		for(ContractState s : allContracts) {
			// add your own contracts but not yet configured if they have balance (so you can withdraw)
			if(s.getCreator().equals(g.getAddress()) && s.getMarket() == 0 && s.getBalance().longValue() > 0L) {
				if(s.getType() == ContractState.Type.SELL)
					contracts.add(s);
				else if(s.getType() == ContractState.Type.BUY)
					contractsBuy.add(s);
				continue;
			}

			// only contracts for this market
			if(s.getMarket() != market.getID())
				continue;
			
			if(onlyMine && !s.getCreator().equals(g.getAddress()) && s.getTaker()!=g.getAddress().getSignedLongId())
				continue;

			// FIXME: add more validity tests here
			if(s.hasPending() ||
					s.getAmountNQT() > 0	&& s.getRate() > 0 && (s.getMarketAccount() != null || s.getType() == ContractState.Type.BUY) &&
					(s.getState() == SellContract.STATE_OPEN
					|| (s.getState()!= SellContract.STATE_FINISHED && s.getTaker() == g.getAddress().getSignedLongId())
					|| (s.getState()!= SellContract.STATE_FINISHED && s.getCreator().equals(g.getAddress())) ) ) {
				if(s.getType() == ContractState.Type.BUY)
					contractsBuy.add(s);
				else
					contracts.add(s);
			}
		}

		// sort by rate
		contracts.sort(new Comparator<ContractState>() {
			@Override
			public int compare(ContractState o1, ContractState o2) {
				return (int)(o1.getRate() - o2.getRate());
			}
		});
		contractsBuy.sort(new Comparator<ContractState>() {
			@Override
			public int compare(ContractState o2, ContractState o1) {
				return (int)(o1.getRate() - o2.getRate());
			}
		});

		model.setRowCount(Math.max(contracts.size(), contractsBuy.size()));
		addContracts(contracts, ASK_COLS);
		addContracts(contractsBuy, BID_COLS);
	}
	
	private void addContracts(ArrayList<ContractState> contracts, int []cols) {
		Globals g = Globals.getInstance();

		// Update the contents
		for (int row = 0; row < contracts.size(); row++) {			
			ContractState s = contracts.get(row);

			String priceFormated = market.format(s.getRate());
			Icon icon = s.getCreator().equals(g.getAddress()) ? editIcon : null; // takeIcon;
			if(s.hasPending()) {
				priceFormated = tr("book_pending_button");
				icon = null;
			}
			else if(s.getTaker() == g.getAddress().getSignedLongId() && s.hasStateFlag(SellContract.STATE_WAITING_PAYMT)) {
				priceFormated = tr(s.getType() == ContractState.Type.BUY ? "book_signal_button" : "book_deposit_button", market);
				icon = null;
			}
			else if(s.getCreator().equals(g.getAddress()) && s.hasStateFlag(SellContract.STATE_WAITING_PAYMT)) {
				priceFormated = tr(s.getType() == ContractState.Type.BUY ? "book_deposit_button" : "book_signal_button", market);
				icon = null;
			}
			JButton b = new ActionButton(priceFormated, s, false);
			b.setIcon(icon);
			b.setBackground(s.getType() == ContractState.Type.BUY ? HistoryPanel.GREEN : HistoryPanel.RED);
			model.setValueAt(b, row, cols[COL_PRICE]);

			long securityPercent = s.getSecurityNQT()*100L / s.getAmountNQT();
			String sizeString = s.getAmount() + " (" + securityPercent + "%)";

			model.setValueAt(sizeString, row, cols[COL_SIZE]);
			double amount = ((double)s.getRate())*s.getAmountNQT();
			amount /= Contract.ONE_BURST;
			model.setValueAt(market.format((long)amount),
					row, cols[COL_TOTAL]);
			ExplorerButton exp = new ExplorerButton(s.getAddress().getRawAddress(), copyIcon, expIcon,
					ExplorerButton.TYPE_ADDRESS, s.getAddress().getID(), s.getAddress().getFullAddress(), BUTTON_EDITOR); 
			if(s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId()
					&& s.getBalance().longValue() > 0 && s.getState() < SellContract.STATE_WAITING_PAYMT
					&& !s.hasPending()) {
				ActionButton withDrawButton = new ActionButton("", s, true);
				withDrawButton.setToolTipText(tr("book_withdraw"));
				withDrawButton.setIcon(cancelIcon);
				exp.add(withDrawButton, BorderLayout.WEST);
			}
			model.setValueAt(exp, row, cols[COL_CONTRACT]);

			//			model.setValueAt(new ExplorerButton(
			//					s.getCreator().getSignedLongId()==g.getAddress().getSignedLongId() ? "YOU" : s.getCreator().getRawAddress(), copyIcon, expIcon,
			//					ExplorerButton.TYPE_ADDRESS, s.getCreator().getID(), s.getCreator().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);

			//			model.setValueAt(s.getSecurity(), row, COL_SECURITY);			
		}
	}
}
