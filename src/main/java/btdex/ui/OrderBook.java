package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Mediators;
import btdex.sc.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.AssetTrade;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class OrderBook extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon, expIcon, upIcon, downIcon;
	JCheckBox listOnlyMine;
	JLabel lastPrice;
	JButton buyButton, sellButton;
	AssetOrder firstBid, firstAsk;
	
	int ROW_HEIGHT;

	HashMap<BurstAddress, ContractState> contractsMap = new HashMap<>();
	BurstID mostRecentID;
	ArrayList<ContractState> marketContracts = new ArrayList<>();

	public static final int COL_CONTRACT = 0;
	public static final int COL_TOTAL = 1;
	public static final int COL_SIZE = 2;
	public static final int COL_PRICE = 3;
	
	public static final int[] BID_COLS = {0, 1, 2, 3};
	public static final int[] ASK_COLS = {7, 6, 5, 4};

//	public static final int COL_ACCOUNT = 5;
//	public static final int COL_SECURITY = 3;
//	public static final int COL_ACTION = 3;

	String[] columnNames = {
			"CONTRACT",
			"TOTAL",
			"SIZE",
			"PRICE",

			"PRICE",
			"SIZE",
			"TOTAL",
			"CONTRACT",
	};

	Market market = null;
	
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
			if(col == BID_COLS[COL_PRICE] || col == ASK_COLS[COL_PRICE] ||
					col == BID_COLS[COL_TOTAL] || col == ASK_COLS[COL_TOTAL])
				colName += " (" + (isToken ? "BURST" : market) + ")";
			if(col == BID_COLS[COL_SIZE] || col == ASK_COLS[COL_SIZE])		
				colName += " (" + (isToken ? market : "BURST") + ")";
			if((col == BID_COLS[COL_CONTRACT] || col==ASK_COLS[COL_CONTRACT]) && isToken)
				colName = "ORDER";
//			if(col == COL_SECURITY && isToken)
//				colName = "TYPE";
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
		boolean cancel;
		
		public ActionButton(String text, ContractState contract, boolean cancel) {
			this(text, null, contract, cancel);
		}
		
		public ActionButton(String text, AssetOrder order, boolean cancel) {
			this(text, order, null, cancel);
		}
		
		public ActionButton(String text, AssetOrder order, ContractState contract, boolean cancel) {
			super(text);
			this.order = order;
			this.contract = contract;
			this.cancel = cancel;
			
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);
					
					if(cancel) {
						CancelOrderDialog dlg = new CancelOrderDialog(f, market, order);
						dlg.setLocationRelativeTo(OrderBook.this);
						dlg.setVisible(true);
					}
					else if(order != null) {
						PlaceOrderDialog dlg = new PlaceOrderDialog(f, market, order);
						dlg.setLocationRelativeTo(OrderBook.this);
						dlg.setVisible(true);
					}
					
					BUTTON_EDITOR.stopCellEditing();
				}
			});
		}
	}

	public void setMarket(Market m) {
		this.market = m;
		
		buyButton.setText("BUY " + m);
		sellButton.setText("SELL " + m);

		// update the column headers
		for (int c = 0; c < columnNames.length; c++) {
			table.getColumnModel().getColumn(c).setHeaderValue(model.getColumnName(c));
		}
		model.setRowCount(0);
		model.fireTableDataChanged();
	}

	public OrderBook(Main main, Market m) {
		super(new BorderLayout());
		
		listOnlyMine = new JCheckBox("List my orders only");
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
		
		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());
		expIcon = IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, 12, table.getForeground());
		upIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_UP, 18, HistoryPanel.GREEN);
		downIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_DOWN, 18, HistoryPanel.RED);

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

		table.getColumnModel().getColumn(BID_COLS[COL_CONTRACT]).setPreferredWidth(200);
		table.getColumnModel().getColumn(ASK_COLS[COL_CONTRACT]).setPreferredWidth(200);
//		table.getColumnModel().getColumn(COL_ACCOUNT).setPreferredWidth(200);
		
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new Desc("Last price", lastPrice = new JLabel()));
		
		top.add(new Desc(" ", buyButton = new JButton()));
		top.add(new Desc(" ", sellButton = new JButton()));

		top.add(new Desc(" ", listOnlyMine));

		buyButton.setBackground(HistoryPanel.GREEN);
		sellButton.setBackground(HistoryPanel.RED);
		buyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);
				PlaceOrderDialog dlg = new PlaceOrderDialog(f, market, firstAsk);
				dlg.setLocationRelativeTo(OrderBook.this);
				dlg.setVisible(true);
			}
		});
		sellButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);
				PlaceOrderDialog dlg = new PlaceOrderDialog(f, market, firstBid);
				dlg.setLocationRelativeTo(OrderBook.this);
				dlg.setVisible(true);				
			}
		});

		add(top, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
		
		setMarket(m);
	}

	public void update() {
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
			String priceLabel = ContractState.format(lastTrade.getPrice().longValue()*market.getFactor()) + " BURST";
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

			String priceFormated = ContractState.format(price*market.getFactor());
			JButton b = new ActionButton(priceFormated, o, false);
			b.setBackground(o.getType() == AssetOrder.OrderType.ASK ? HistoryPanel.RED : HistoryPanel.GREEN);
			model.setValueAt(b, row, cols[COL_PRICE]);
			
			model.setValueAt(market.format(amountToken), row, cols[COL_SIZE]);
			model.setValueAt(ContractState.format(amountToken*price), row, cols[COL_TOTAL]);

			model.setValueAt(new ExplorerButton(o.getId().getID(), copyIcon, expIcon, BUTTON_EDITOR), row, cols[COL_CONTRACT]);

			if(o.getAccountAddress().getSignedLongId() == Globals.getInstance().getAddress().getSignedLongId())
				model.setValueAt(new ActionButton("CANCEL", o, true), row, cols[COL_CONTRACT]);
		}
		// fill with null all the remaining rows
		for (; row < model.getRowCount(); row++) {
			for (int col = 0; col < cols.length; col++) {
				model.setValueAt(null, row, cols[col]);
			}
		}
	}

	private void updateContracts() {
		mostRecentID = ContractState.addContracts(contractsMap, mostRecentID);

		Globals g = Globals.getInstance();
		Mediators m = new Mediators(g.isTestnet());
		marketContracts.clear();
		for(ContractState s : contractsMap.values()) {
			// if not the right market, do not update
			if(s.getMarket() != market.getID())
				continue;
			
			s.update();
			// FIXME: add more validity tests here
			if(s.getAmountNQT() > 0
					&& s.getState() == SellContract.STATE_OPEN
					&& Constants.FEE_CONTRACT == s.getFeeContract()
					&& m.isMediatorAccepted(s.getMediator1())
					&& m.isMediatorAccepted(s.getMediator2()) )
				marketContracts.add(s);
		}
		
		// sort by rate
		marketContracts.sort(new Comparator<ContractState>() {
			@Override
			public int compare(ContractState o1, ContractState o2) {
				return (int)(o1.getRate() - o2.getRate());
			}
		});

		model.setRowCount(marketContracts.size());

		// Update the contents
		for (int row = 0; row < marketContracts.size(); row++) {			
			ContractState s = marketContracts.get(row);
			
			model.setValueAt(market.format(s.getRate()), row, COL_PRICE);
			model.setValueAt(s.getAmount(), row, COL_SIZE);
			model.setValueAt(market.format((s.getRate()*s.getAmountNQT()) / Contract.ONE_BURST),
					row, COL_TOTAL);
			model.setValueAt(new ExplorerButton(s.getAddress().getRawAddress(), copyIcon, expIcon,
					ExplorerButton.TYPE_ADDRESS, s.getAddress().getID(), s.getAddress().getFullAddress(), BUTTON_EDITOR), row, COL_CONTRACT);
			
//			model.setValueAt(new ExplorerButton(
//					s.getCreator().getSignedLongId()==g.getAddress().getSignedLongId() ? "YOU" : s.getCreator().getRawAddress(), copyIcon, expIcon,
//					ExplorerButton.TYPE_ADDRESS, s.getCreator().getID(), s.getCreator().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);

//			model.setValueAt(s.getSecurity(), row, COL_SECURITY);
			
			if(s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId())
				model.setValueAt(new ActionButton("CANCEL", s, true), row, COL_CONTRACT);
			else
				model.setValueAt(new ActionButton("BUY BURST", s, false), row, COL_CONTRACT);
			
		}
	}
}
