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

import bt.Contract;
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.sm.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.Order;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class OrderBook extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	
	Icon copyIcon;

	HashMap<BurstAddress, ContractState> contractsMap = new HashMap<>();
	ArrayList<ContractState> marketContracts = new ArrayList<>();

	public static final int COL_PRICE = 0;
	public static final int COL_SIZE = 1;
	public static final int COL_TOTAL = 2;
	public static final int COL_CONTRACT = 3;
	public static final int COL_SECURITY = 4;
	public static final int COL_ACTION = 5;

	String[] columnNames = {
			"PRICE",
			"SIZE",
			"TOTAL",
			"CONTRACT",
			"SECURITY",
			"ACTION"
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
			if(col == COL_SECURITY && isToken)
				colName = "TYPE";
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_ACTION || col == COL_CONTRACT;
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
		
		Order order;
		ContractState contract;
		boolean cancel;
		
		public ActionButton(String text, ContractState contract, boolean cancel) {
			this(text, null, contract, cancel);
		}
		
		public ActionButton(String text, Order order, boolean cancel) {
			this(text, order, null, cancel);
		}
		
		public ActionButton(String text, Order order, ContractState contract, boolean cancel) {
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
		model.fireTableDataChanged();

		update();
	}

	public OrderBook(Market m) {
		super(new BorderLayout());

		market = m;

		table = new JTable(model = new MyTableModel());
		table.setRowHeight(table.getRowHeight()+7);
		
		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());

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

		table.getColumnModel().getColumn(COL_ACTION).setCellRenderer(BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_ACTION).setCellEditor(BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_CONTRACT).setCellRenderer(BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_CONTRACT).setCellEditor(BUTTON_EDITOR);

		table.getColumnModel().getColumn(COL_CONTRACT).setPreferredWidth(200);

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

		ArrayList<Order> orders = new ArrayList<>();
		Order[] bids = g.getNS().getBidOrders(token).blockingGet();
		Order[] asks = g.getNS().getAskOrders(token).blockingGet();
		
		for (int i = 0; i < asks.length; i++) {
			orders.add(asks[i]);
		}
		for (int i = 0; i < bids.length; i++) {
			orders.add(bids[i]);
		}
		
		// sort by price
		orders.sort(new Comparator<Order>() {
			@Override
			public int compare(Order o1, Order o2) {
				return (int)(o1.getPrice().doubleValue() - o2.getPrice().doubleValue());
			}
		});


		model.setRowCount(orders.size());

		// Update the contents
		for (int row = 0; row < orders.size(); row++) {
			Order o = orders.get(row);

			// price always come in Burst, so no problem in this division using long's
			long priceBurst = o.getPrice().longValue()/market.getFactor();
			long amountToken = o.getQuantity().longValue();
			
			if(priceBurst == 0 || amountToken == 0)
				continue;

//			long priceToken = (100000000L)/priceBurst;
//			long amountPlank = (amountToken * priceBurst);

			model.setValueAt(ContractState.format(priceBurst), row, COL_PRICE);
			model.setValueAt(market.format(amountToken), row, COL_SIZE);
			model.setValueAt(ContractState.format((amountToken*priceBurst)/market.getFactor()), row, COL_TOTAL);

			model.setValueAt(new CopyToClipboardButton(o.getOrder().getID(), copyIcon, BUTTON_EDITOR), row, COL_CONTRACT);
			
			if(o.getType().equals("bid")) {
				model.setValueAt("BUYING " + market, row, COL_SECURITY);
				model.setValueAt(new ActionButton("SELL " + market, o, false), row, COL_ACTION);
			}
			else {
				model.setValueAt("SELLING " + market, row, COL_SECURITY);
				model.setValueAt(new ActionButton("BUY " + market, o, false), row, COL_ACTION);
			}
			
			if(o.getAccount().getSignedLongId() == g.getAddress().getSignedLongId())
				model.setValueAt(new ActionButton("CANCEL", o, true), row, COL_ACTION);
		}
	}

	private void updateContracts() {
		ContractState.addContracts(contractsMap);

		Globals g = Globals.getInstance();

		marketContracts.clear();
		for(ContractState s : contractsMap.values()) {
			if(s.getMarket() == market.getID() && s.getAmountNQT() > 0
					&& s.getState() == SellContract.STATE_OPEN
					&& g.isArbitratorAccepted(s.getArbitrator1())
					&& g.isArbitratorAccepted(s.getArbitrator2()) )
				marketContracts.add(s);
		}

		// sort by rate
		marketContracts.sort(new Comparator<ContractState>() {
			@Override
			public int compare(ContractState o1, ContractState o2) {
				return (int)(o1.getRate() - o2.getRate());
			}
		});

		for (ContractState s : marketContracts) {
			s.update();
		}

		model.setRowCount(marketContracts.size());

		// Update the contents
		for (int row = 0; row < marketContracts.size(); row++) {			
			ContractState s = marketContracts.get(row);
			
			model.setValueAt(market.format(s.getRate()), row, COL_PRICE);
			model.setValueAt(s.getAmount(), row, COL_SIZE);
			model.setValueAt(market.format((s.getRate()*s.getAmountNQT()) / Contract.ONE_BURST),
					row, COL_TOTAL);
			model.setValueAt(new CopyToClipboardButton(s.getAddress().getRawAddress(), copyIcon,
					s.getAddress().getFullAddress(), BUTTON_EDITOR), row, COL_CONTRACT);
			model.setValueAt(s.getSecurity(), row, COL_SECURITY);
			
			if(s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId())
				model.setValueAt(new ActionButton("CANCEL", s, true), row, COL_ACTION);
			else
				model.setValueAt(new ActionButton("BUY BURST", s, false), row, COL_ACTION);
			
		}
	}
}
