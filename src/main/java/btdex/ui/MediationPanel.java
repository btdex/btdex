package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import btdex.core.ContractState;
import btdex.core.ContractType;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.core.NumberFormatting;
import btdex.sc.SellContract;
import btdex.ui.orderbook.ActionButton;
import btdex.ui.orderbook.BookTable;
import burst.kit.entity.BurstAddress;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class MediationPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon, expIcon, upIcon, downIcon;
	JCheckBox listOnlyMine;
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");

	public static final int COL_MARKET = 0;
	public static final int COL_PRICE = 1;
	public static final int COL_AMOUNT = 2;
	public static final int COL_CONTRACT = 3;
	public static final int COL_MAKER = 4;
	public static final int COL_TAKER = 5;
	public static final int COL_MEDIATE = 6;

	String[] columnNames = {
			"acc_market_col",
			"book_price",
			"book_size",
			"book_contract",
			"med_maker",
			"med_taker",
			"med_action",
	};

	class MyTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int col) {
			String colName = columnNames[col];
			
			if(col == COL_PRICE || col == COL_AMOUNT)
				colName = tr(colName, "");
			else
				colName = tr(colName);
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_CONTRACT || col == COL_MAKER || col == COL_TAKER || col == COL_MEDIATE;
		}
	}

	public MediationPanel(Main main) {
		super(new BorderLayout());

		JPanel top = new JPanel(new BorderLayout());
		JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(topLeft, BorderLayout.LINE_START);
		
		topLeft.add(listOnlyMine = new JCheckBox(tr("hist_list_mine_only")));
		listOnlyMine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.update();
			}
		});

		table = new JTable(model = new MyTableModel());
		table.setRowSelectionAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowHeight(table.getRowHeight()+10);
		table.setPreferredScrollableViewportSize(new Dimension(200, 200));

		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());
		expIcon = IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, 12, table.getForeground());
		
		JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		top.add(topRight, BorderLayout.LINE_END);

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

		table.getColumnModel().getColumn(COL_CONTRACT).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_CONTRACT).setCellEditor(BookTable.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_MAKER).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_MAKER).setCellEditor(BookTable.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_TAKER).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_TAKER).setCellEditor(BookTable.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_MEDIATE).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_MEDIATE).setCellEditor(BookTable.BUTTON_EDITOR);
		//
		table.getColumnModel().getColumn(COL_CONTRACT).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_MAKER).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_TAKER).setPreferredWidth(200);

		add(top, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void update() {
		Globals g = Globals.getInstance();
		
		boolean onlyMine = listOnlyMine.isSelected();

		Collection<ContractState> allContracts = Contracts.getContracts();
		ArrayList<ContractState> contracts = new ArrayList<>();
		
		// Only open orders with a taker and under our supervision
		for(ContractState s : allContracts) {
			if(s.getState() == SellContract.STATE_FINISHED || s.getTaker() == 0L)
				continue;
			if(onlyMine && s.getMediator1()!=g.getAddress().getSignedLongId() && s.getMediator2()!=g.getAddress().getSignedLongId())
				continue;
			if(Markets.findMarket(s.getMarket()) == null)
				continue;

			contracts.add(s);
		}

		model.setRowCount(contracts.size());

		// Update the contents
		for (int row = 0, i=0; i < contracts.size(); i++) {
			ContractState s = contracts.get(row);
			
			Market market = Markets.findMarket(s.getMarket());
			
			long amount = s.getAmountNQT();
			double price = (double)s.getRate() / market.getFactor();
			
			String type = tr(s.getType() == ContractType.BUY ? "offer_buy_burst_with" : "offer_sell_burst_for", market.toString());
			model.setValueAt(type, row, COL_MARKET);
			
			BurstAddress maker = s.getCreator();
			BurstAddress taker = BurstAddress.fromId(s.getTaker());

			model.setValueAt(new ExplorerButton(maker.getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, maker.getID(), maker.getFullAddress()), row, COL_MAKER);
			model.setValueAt(new ExplorerButton(taker.getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, taker.getID(), taker.getFullAddress()), row, COL_TAKER);

			model.setValueAt(new ExplorerButton(
					s.getAddress().getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, s.getAddress().getID(),
							s.getAddress().getFullAddress()), row, COL_CONTRACT);

			model.setValueAt(market.getNumberFormat().format(price), row, COL_PRICE);
			model.setValueAt(NumberFormatting.BURST.format(amount), row, COL_AMOUNT);
			
			
			JButton b = new ActionButton(this, market, tr("med_details"), null, s, false, false, false);
			if(s.getState() > SellContract.STATE_DISPUTE) {
				// under dispute
				b.setBackground(HistoryPanel.RED);
				if(s.getMediator1() == g.getAddress().getSignedLongId() || s.getMediator2() == g.getAddress().getSignedLongId() ){
					// under dispute and we are the mediator
					b.setText(tr("med_mediate"));
				}
			}
			model.setValueAt(b, row, COL_MEDIATE);

			row++;
		}
		model.fireTableDataChanged();
	}
}
