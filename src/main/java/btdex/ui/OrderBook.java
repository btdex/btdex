package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
			"SIZE (BURST)",
			"TOTAL",
			"CONTRACT",
			"SECURITY (BURST)",
			"ACTION"
	};

	Icon TAKE_ICON = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 12);

	Market selectedMarket = null;

	class MyTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int col) {
			String colName = columnNames[col];
			if(col == COL_PRICE || col == COL_TOTAL)
				colName += " (" + selectedMarket.toString() + ")";
			if(col == COL_CONTRACT && selectedMarket.getTokenID()!=null)
				colName = "ORDER";
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_ACTION || col == COL_CONTRACT;
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

	public void setMarket(Market m) {
		this.selectedMarket = m;

		// update the column headers
		for (int c = 0; c < columnNames.length; c++) {
			table.getColumnModel().getColumn(c).setHeaderValue(model.getColumnName(c));
		}
		model.fireTableDataChanged();

		update();
	}

	public OrderBook(Market m) {
		super(new BorderLayout());

		selectedMarket = m;

		table = new JTable(model = new MyTableModel());

		table.setRowHeight(table.getRowHeight()+7);

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

		table.getColumnModel().getColumn(COL_ACTION).setCellRenderer(RENDERER);
		table.getColumnModel().getColumn(COL_ACTION).setCellEditor(EDITOR);
		table.getColumnModel().getColumn(COL_CONTRACT).setCellRenderer(RENDERER);
		table.getColumnModel().getColumn(COL_CONTRACT).setCellEditor(EDITOR);

		table.getColumnModel().getColumn(COL_CONTRACT).setPreferredWidth(200);

		add(scrollPane, BorderLayout.CENTER);
	}

	public void update() {
		if(selectedMarket.getTokenID()==null) {
			updateContracts();
		}
		else
			updateOrders();
	}

	private void updateOrders() {
		BurstID token = selectedMarket.getTokenID();

		Globals g = Globals.getInstance();

		Order[] orders = g.getNS().getBidOrders(token).blockingGet();

		model.setRowCount(orders.length);

		// Update the contents
		for (int row = 0; row < orders.length; row++) {
			Order o = orders[row];

			// price always come in Burst, so no problem in this division using long's
			long priceBurst = o.getPrice().longValue()/100000000L;
			long amountToken = o.getQuantity().longValue();

			long priceToken = (100000000L)/priceBurst;
			long amountPlank = (amountToken * priceBurst);

			model.setValueAt(selectedMarket.numberFormat(priceToken), row, COL_PRICE);
			model.setValueAt(ContractState.format(amountPlank), row, COL_SIZE);
			model.setValueAt(selectedMarket.numberFormat(amountToken), row, COL_TOTAL);

			model.setValueAt(new JButton(o.getOrder().getID()), row, COL_CONTRACT);
			model.setValueAt("0", row, COL_SECURITY);
			model.setValueAt(new JButton("Take"), row, COL_ACTION);
		}
	}

	private void updateContracts() {
		ContractState.addContracts(contractsMap);

		Globals g = Globals.getInstance();

		marketContracts.clear();
		for(ContractState s : contractsMap.values()) {
			if(s.getMarket() == selectedMarket.getID() && s.getAmountNQT() > 0
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
			for (int col = 0; col < model.getColumnCount(); col++) {
				ContractState s = marketContracts.get(row);
				switch (col) {
				case 0:
					model.setValueAt(selectedMarket.numberFormat(s.getRate()), row, col);
					break;
				case 1:
					model.setValueAt(s.getAmount(), row, col);
					break;
				case 2:
					model.setValueAt(selectedMarket.numberFormat((s.getRate()*s.getAmountNQT()) / Contract.ONE_BURST),
							row, col);
					break;
				case COL_CONTRACT:
					model.setValueAt(new JButton(s.getAddress().getRawAddress()), row, col);
					break;
				case COL_SECURITY:
					model.setValueAt(s.getSecurity(), row, col);
					break;
				case COL_ACTION:
					model.setValueAt(new JButton("Take"), row, col);
					break;
					//				return new JButton("Take", TAKE_ICON);
				default:
					break;
				}				
			}
		}
	}
}
