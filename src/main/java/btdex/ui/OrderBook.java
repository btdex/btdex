package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;
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
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
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
	Icon copyIcon, upIcon, downIcon;
	JCheckBox listOnlyMine;
	int lastPriceRow;
	
	int ROW_HEIGHT;

	HashMap<BurstAddress, ContractState> contractsMap = new HashMap<>();
	BurstID mostRecentID;
	ArrayList<ContractState> marketContracts = new ArrayList<>();

	public static final int COL_PRICE = 0;
	public static final int COL_SIZE = 1;
	public static final int COL_TOTAL = 2;
	public static final int COL_CONTRACT = 4;
	public static final int COL_ACCOUNT = 5;
//	public static final int COL_SECURITY = 3;
	public static final int COL_ACTION = 3;

	String[] columnNames = {
			"PRICE",
			"SIZE",
			"TOTAL",
//			"SECURITY",
			"ACTION",
			"CONTRACT",
			"ACCOUNT",
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
			if(col == COL_PRICE || col == COL_TOTAL)
				colName += " (" + (isToken ? "BURST" : market) + ")";
			if(col == COL_SIZE)				
				colName += " (" + (isToken ? market : "BURST") + ")";
			if(col == COL_CONTRACT && isToken)
				colName = "ORDER";
//			if(col == COL_SECURITY && isToken)
//				colName = "TYPE";
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_ACTION || col == COL_CONTRACT || col == COL_ACCOUNT;
		}
	}
	
	class MyTable extends JTable {
		private static final long serialVersionUID = 3251005544025726619L;

		public MyTable(DefaultTableModel model) {
			super(model);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if(column == COL_ACTION || column == COL_CONTRACT || column == COL_ACCOUNT
					|| row == lastPriceRow)
				return BUTTON_RENDERER;
			
			return super.getCellRenderer(row, column);
		}
		
		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if(column == COL_ACTION || column == COL_CONTRACT || column == COL_ACCOUNT)
				return BUTTON_EDITOR;
			
			return super.getCellEditor(row, column);
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
		
		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());
		upIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_UP, 18, HistoryPanel.GREEN);
		downIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_DOWN, 18, HistoryPanel.RED);

		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);

		// Center header and all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for (int i = 0; i < table.getColumnCount()-1; i++) {
			table.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );			
		}
		JTableHeader jtableHeader = table.getTableHeader();
		DefaultTableCellRenderer rend = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);

		table.setAutoCreateColumnsFromModel(false);

		table.getColumnModel().getColumn(COL_CONTRACT).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_ACCOUNT).setPreferredWidth(200);

		add(listOnlyMine, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
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

		ArrayList<AssetOrder> orders = new ArrayList<>();
		AssetOrder[] bids = g.getNS().getBidOrders(token).blockingGet();
		AssetOrder[] asks = g.getNS().getAskOrders(token).blockingGet();
		
		for (int i = 0; i < asks.length; i++) {
			if(onlyMine && asks[i].getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
				continue;
			orders.add(asks[i]);
		}
		for (int i = 0; i < bids.length; i++) {
			if(onlyMine && bids[i].getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
				continue;
			orders.add(bids[i]);
		}
		
		// sort by price
		orders.sort(new Comparator<AssetOrder>() {
			@Override
			public int compare(AssetOrder o1, AssetOrder o2) {
				int cmp = (int)(o2.getPrice().longValue() - o1.getPrice().longValue());
				if(cmp == 0)
					cmp = o2.getHeight() - o1.getHeight();
				return cmp;
			}
		});

		model.setRowCount(orders.size() + 1);
		
		boolean lastPriceAdded = false;
		AssetTrade trs[] = g.getNS().getAssetTrades(market.getTokenID(), null, 0, 2).blockingGet();
		AssetTrade lastTrade = trs.length > 0 ? trs[0] : null;
		boolean lastIsUp = true;
		if(trs.length > 1 && trs[1].getPrice().longValue() < trs[1].getPrice().longValue())
			lastIsUp = false;
		
		// Update the contents
		for (int i = 0, row = 0; i < orders.size(); i++, row++) {
			AssetOrder o = orders.get(i);
			
			if(lastPriceAdded == false && o.getType() == AssetOrder.OrderType.BID) {
				lastPriceRow = row;
				table.setRowHeight(row, ROW_HEIGHT*2);
				
				JButton lastPriceButton = new ExplorerButton(ContractState.format(lastTrade.getPrice().longValue()*market.getFactor()),
						lastIsUp ? upIcon : downIcon, ExplorerButton.TYPE_TRANSACTION, lastTrade.getAskOrderId().getID(),
						BUTTON_EDITOR);
				lastPriceButton.setToolTipText("Last trade price");
				lastPriceButton.setForeground(lastIsUp ? HistoryPanel.GREEN : HistoryPanel.RED);
				model.setValueAt(lastPriceButton, row, COL_PRICE);

				model.setValueAt(null, row, COL_SIZE);
				model.setValueAt(null, row, COL_TOTAL);
				model.setValueAt(new ExplorerButton(lastTrade.getAskOrderId().getID(), copyIcon),
						row, COL_CONTRACT);
				model.setValueAt(null, row, COL_ACCOUNT);
//				model.setValueAt(null, row, COL_SECURITY);
				model.setValueAt(null, row, COL_ACTION);
				
				row++;
				lastPriceAdded = true;
			}
			table.setRowHeight(row, ROW_HEIGHT);

			// price always come in Burst, so no problem in this division using long's
			long price = o.getPrice().longValue();
			long amountToken = o.getQuantity().longValue();
			
			if(price == 0 || amountToken == 0)
				continue;

			model.setValueAt(ContractState.format(price*market.getFactor()), row, COL_PRICE);
			model.setValueAt(market.format(amountToken), row, COL_SIZE);
			model.setValueAt(ContractState.format(amountToken*price), row, COL_TOTAL);

			model.setValueAt(new ExplorerButton(o.getId().getID(), copyIcon, BUTTON_EDITOR), row, COL_CONTRACT);
			model.setValueAt(new ExplorerButton(
					o.getAccountAddress().getSignedLongId()==g.getAddress().getSignedLongId() ? "YOU" : o.getAccountAddress().getRawAddress(), copyIcon,
					ExplorerButton.TYPE_ADDRESS, o.getAccountAddress().getID(), o.getAccountAddress().getFullAddress(), BUTTON_EDITOR), row, COL_ACCOUNT);

			if(o.getType() == AssetOrder.OrderType.BID) {
//				model.setValueAt("BUYING " + market, row, COL_SECURITY);
				JButton b = new ActionButton("SELL " + market, o, false);
				b.setBackground(HistoryPanel.RED);
				model.setValueAt(b, row, COL_ACTION);
			}
			else {
//				model.setValueAt("SELLING " + market, row, COL_SECURITY);
				JButton b = new ActionButton("BUY " + market, o, false);
				b.setBackground(HistoryPanel.GREEN);
				model.setValueAt(b, row, COL_ACTION);
			}
			
			if(o.getAccountAddress().getSignedLongId() == g.getAddress().getSignedLongId())
				model.setValueAt(new ActionButton("CANCEL", o, true), row, COL_ACTION);
		}
	}

	private void updateContracts() {
		mostRecentID = ContractState.addContracts(contractsMap, mostRecentID);

		Globals g = Globals.getInstance();

		marketContracts.clear();
		for(ContractState s : contractsMap.values()) {
			// if not the right market, do not update
			if(s.getMarket() != market.getID())
				continue;
			
			s.update();
			// FIXME: add more validity tests here
			if(s.getAmountNQT() > 0
					&& s.getState() == SellContract.STATE_OPEN
					&& g.getFeeContract() == s.getFeeContract()
					&& g.isArbitratorAccepted(s.getMediator1())
					&& g.isArbitratorAccepted(s.getMediator2()) )
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
			model.setValueAt(new ExplorerButton(s.getAddress().getRawAddress(), copyIcon,
					ExplorerButton.TYPE_ADDRESS, s.getAddress().getID(), s.getAddress().getFullAddress(), BUTTON_EDITOR), row, COL_CONTRACT);
			
			model.setValueAt(new ExplorerButton(
					s.getCreator().getSignedLongId()==g.getAddress().getSignedLongId() ? "YOU" : s.getCreator().getRawAddress(), copyIcon,
					ExplorerButton.TYPE_ADDRESS, s.getCreator().getID(), s.getCreator().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);

//			model.setValueAt(s.getSecurity(), row, COL_SECURITY);
			
			if(s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId())
				model.setValueAt(new ActionButton("CANCEL", s, true), row, COL_ACTION);
			else
				model.setValueAt(new ActionButton("BUY BURST", s, false), row, COL_ACTION);
			
		}
	}
}
