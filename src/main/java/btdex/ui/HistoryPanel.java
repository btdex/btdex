package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;

import btdex.core.BurstNode;
import btdex.core.ContractState;
import btdex.core.ContractTrade;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.AssetTrade;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class HistoryPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon, expIcon, upIcon, downIcon;
	JCheckBox listOnlyMine;
	JLabel lastPrice;
	private OrderBook book;

	public static final Color RED = Color.decode("#BE474A");
	public static final Color GREEN = Color.decode("#29BF76");

	Market market = null, newMarket;

	private JFreeChart chart;

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");

	public static final int COL_PRICE = 0;
	public static final int COL_AMOUNT = 1;
	public static final int COL_TIME = 2;
	public static final int COL_BUYER = 3;
	public static final int COL_SELLER = 4;

	String[] columnNames = {
			"book_price",
			"book_size",
			"hist_time",
			"hist_buyer",
			"hist_seller",
	};

	class MyTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int col) {
			String colName = columnNames[col];
			if(market == null)
				return colName;
			
			boolean isToken = market.getTokenID()!=null;
			if(col == COL_PRICE)
				colName = tr(colName, isToken ? "BURST" : market);
			else if(col == COL_AMOUNT)
				colName = tr(colName, isToken ? market : "BURST");
			else
				colName = tr(colName);
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_BUYER || col == COL_SELLER;
		}
	}

	public HistoryPanel(Main main, Market market, OrderBook book) {
		super(new BorderLayout());

		this.book = book;
		
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(lastPrice = new JLabel());
		lastPrice.setToolTipText(tr("book_last_price"));
		top.add(listOnlyMine = new JCheckBox(tr("hist_list_mine_only")));
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

		setMarket(market);

		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());
		expIcon = IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, 12, table.getForeground());
		upIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_UP, 18, HistoryPanel.GREEN);
		downIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_DOWN, 18, HistoryPanel.RED);

		ChartPanel chartPanel = null;
		chart = ChartFactory.createCandlestickChart(null, null, null, null, true);
		chart.getXYPlot().setOrientation(PlotOrientation.VERTICAL);
		chart.removeLegend();
		chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(200, 200));

		chart.setBackgroundPaint(table.getBackground());
		chart.setBorderPaint(table.getForeground());
		chart.getXYPlot().setBackgroundPaint(table.getBackground());
		NumberAxis raxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
		raxis.setAutoRangeIncludesZero(false);
		raxis.setTickLabelPaint(table.getForeground());
		raxis.setLabelPaint(table.getForeground());
		chart.getXYPlot().getDomainAxis().setTickLabelPaint(table.getForeground());
		chart.getXYPlot().getDomainAxis().setLabelPaint(table.getForeground());
		CandlestickRenderer r = (CandlestickRenderer) chart.getXYPlot().getRenderer();
		r.setDownPaint(RED);
		r.setUpPaint(GREEN);
		r.setSeriesPaint(0, table.getForeground());

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

		table.getColumnModel().getColumn(COL_BUYER).setCellRenderer(OrderBook.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_BUYER).setCellEditor(OrderBook.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_SELLER).setCellRenderer(OrderBook.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_SELLER).setCellEditor(OrderBook.BUTTON_EDITOR);
		//
		table.getColumnModel().getColumn(COL_TIME).setPreferredWidth(120);
		table.getColumnModel().getColumn(COL_BUYER).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_SELLER).setPreferredWidth(200);

		add(top, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
		add(chartPanel, BorderLayout.PAGE_END);
	}

	public void setMarket(Market m) {
		newMarket = m;
	}

	public void update() {
		if(newMarket != market) {
			market = newMarket;

			// update the column headers
			for (int c = 0; c < columnNames.length; c++) {
				table.getColumnModel().getColumn(c).setHeaderValue(model.getColumnName(c));
			}
			table.getTableHeader().repaint();
		}

		if(market.getTokenID() != null)
			updateToken();
		else
			updateContract();
		
		model.fireTableDataChanged();
	}

	private void updateToken() {
		Globals g = Globals.getInstance();
		BurstNode bn = BurstNode.getInstance();
		boolean myHistory = listOnlyMine.isSelected();

		AssetTrade trs[] = bn.getAssetTrades(market);
		if(trs == null)
			return;
		
		AssetTrade lastTrade = trs !=null && trs.length > 0 ? trs[0] : null;
		boolean lastIsUp = true;
		if(trs !=null && trs.length > 1 && trs[0].getPrice().longValue() < trs[1].getPrice().longValue())
			lastIsUp = false;

		if(lastTrade != null) {
			// set the last price label
			String priceLabel = NumberFormatting.BURST.format(lastTrade.getPrice().longValue()*market.getFactor()) + " BURST";
			lastPrice.setText(priceLabel);
			lastPrice.setIcon(lastIsUp ? upIcon : downIcon);
			lastPrice.setForeground(lastIsUp ? HistoryPanel.GREEN : HistoryPanel.RED);
			book.setLastPrice(lastPrice.getText(), lastPrice.getIcon(), lastPrice.getForeground());
		}
		else {
			lastPrice.setText("");
			lastPrice.setIcon(null);
			book.setLastPrice(lastPrice.getText(), lastPrice.getIcon(), lastPrice.getForeground());			
		}

		int nLines = 0;

		int maxLines = Math.min(200, trs == null ? 0 : trs.length);
		for (int i = 0; i < maxLines; i++) {
			AssetTrade tr = trs[i];
			if(myHistory &&
					tr.getBuyerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId() &&
					tr.getSellerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId())
				continue;
			nLines++;
		}

		model.setRowCount(nLines);

		// Update the contents
		for (int row = 0, i=0; i < maxLines; i++) {
			AssetTrade tr = trs[i];
			if(myHistory &&
					tr.getBuyerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId() &&
					tr.getSellerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId())
				continue;

			long amount = tr.getQuantity().longValue();
			long price = tr.getPrice().longValue();

			model.setValueAt(new ExplorerButton(
					tr.getBuyerAddress().getSignedLongId()==g.getAddress().getSignedLongId() ? tr("hist_you") : tr.getBuyerAddress().getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, tr.getBuyerAddress().getID(), tr.getBuyerAddress().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_BUYER);
			model.setValueAt(new ExplorerButton(
					tr.getSellerAddress().getSignedLongId()==g.getAddress().getSignedLongId() ? tr("hist_you") : tr.getSellerAddress().getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, tr.getSellerAddress().getID(), tr.getSellerAddress().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_SELLER);

			model.setValueAt(NumberFormatting.BURST.format(price*market.getFactor()), row, COL_PRICE);
			model.setValueAt(market.format(amount), row, COL_AMOUNT);
			model.setValueAt(DATE_FORMAT.format(tr.getTimestamp().getAsDate()), row, COL_TIME);

			row++;
		}
		
		ArrayList<OHLCDataItem> data = new ArrayList<>();
		int NCANDLES = 50;
		long DELTA = TimeUnit.HOURS.toMillis(4);
		Date start = new Date(System.currentTimeMillis() - DELTA*NCANDLES);
		Date next = new Date(start.getTime() + DELTA);

		double lastClose = Double.NaN;
		for (int i = 0; i < 50; i++) {
			double high = 0;
			double low = Double.MAX_VALUE;
			double close = lastClose;
			double open = lastClose;
			double volume = 0;
			if(!Double.isNaN(lastClose))
				high = lastClose;

			for (int row = maxLines-1; row >= 0; row--) {
				AssetTrade tr = trs[row];
				Date date = tr.getTimestamp().getAsDate();
				double price = tr.getPrice().doubleValue()*market.getFactor();

				if(date.after(start) && date.before(next)) {
					close = price;
					open = lastClose;
					high = Math.max(price, high);
					low = Math.min(price, low);
					if(Double.isNaN(open))
						open = lastClose = price;
					volume += tr.getQuantity().doubleValue()*market.getFactor();
				}
			}
			low = Math.min(high, low);
			if(!Double.isNaN(close))
				lastClose = close;

			if(!Double.isNaN(open)) {
				OHLCDataItem item = new OHLCDataItem(
						start, open, high, low, close, volume);
				data.add(item);
			}

			start = next;
			next = new Date(start.getTime() + DELTA);
		}

		DefaultOHLCDataset dataset = new DefaultOHLCDataset(market.toString(), data.toArray(new OHLCDataItem[data.size()]));

		chart.getXYPlot().setDataset(dataset);
	}

	private void updateContract() {
		Globals g = Globals.getInstance();
		boolean myHistory = listOnlyMine.isSelected();

		Collection<ContractState> allContracts = Contracts.getContracts();
		ArrayList<ContractTrade> trades = new ArrayList<>();
		
		// build the trade list for this market
		for(ContractState s : allContracts) {
			for(ContractTrade t : s.getTrades()) {
				if(t.getMarket() == market.getID()) {
					trades.add(t);
					// TODO: break if these trades are old enough
				}
			}
		}
		
		// now we sort the trades on time
		trades.sort(new Comparator<ContractTrade>() {
			@Override
			public int compare(ContractTrade t1, ContractTrade t2) {
				int cmp = (int)(t2.getTimestamp().getAsDate().getTime() - t1.getTimestamp().getAsDate().getTime());
				return cmp;
			}
		});
		
		ContractTrade lastTrade = trades.size() > 1 ? trades.get(0) : null;
		boolean lastIsUp = true;
		if(trades.size() > 1 && trades.get(0).getRate() < trades.get(1).getRate())
			lastIsUp = false;

		if(lastTrade != null) {
			// set the last price label
			String priceLabel = market.getNumberFormat().format(lastTrade.getRate()) + " " + market;
			lastPrice.setText(priceLabel);
			lastPrice.setIcon(lastIsUp ? upIcon : downIcon);
			lastPrice.setForeground(lastIsUp ? HistoryPanel.GREEN : HistoryPanel.RED);
			book.setLastPrice(lastPrice.getText(), lastPrice.getIcon(), lastPrice.getForeground());
		}
		else {
			lastPrice.setText("");
			lastPrice.setIcon(null);
			book.setLastPrice(lastPrice.getText(), lastPrice.getIcon(), lastPrice.getForeground());			
		}
		
		int maxLines = Math.min(200, trades.size());
		int nLines = myHistory ? 0 : maxLines;
		for (int i = 0; myHistory && i < maxLines; i++) {
			ContractTrade tr = trades.get(i);
			if(myHistory && !tr.getCreator().equals(g.getAddress()) && !tr.getTaker().equals(g.getAddress()))
				continue;
			nLines++;
		}

		model.setRowCount(nLines);

		// Update the contents
		for (int row = 0, i=0; i < maxLines; i++) {
			ContractTrade tr = trades.get(i);
			if(myHistory && !tr.getCreator().equals(g.getAddress()) && !tr.getTaker().equals(g.getAddress()))
				continue;

			long amount = tr.getAmount();
			double price = (double)tr.getRate() / market.getFactor();
			
			BurstAddress buyer = tr.getContract().getType() == ContractState.Type.BUY ? tr.getCreator() : tr.getTaker();
			BurstAddress seller = tr.getContract().getType() == ContractState.Type.BUY ? tr.getTaker() : tr.getCreator();

			model.setValueAt(new ExplorerButton(
					buyer.equals(g.getAddress()) ? tr("hist_you") : buyer.getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, buyer.getID(), buyer.getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_BUYER);
			model.setValueAt(new ExplorerButton(
					seller.equals(g.getAddress()) ? tr("hist_you") : seller.getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, seller.getID(), seller.getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_SELLER);

			model.setValueAt(market.getNumberFormat().format(price), row, COL_PRICE);
			model.setValueAt(NumberFormatting.BURST.format(amount), row, COL_AMOUNT);
			model.setValueAt(DATE_FORMAT.format(tr.getTimestamp().getAsDate()), row, COL_TIME);

			row++;
		}

		ArrayList<OHLCDataItem> data = new ArrayList<>();
		int NCANDLES = 50;
		long DELTA = TimeUnit.HOURS.toMillis(4);
		Date start = new Date(System.currentTimeMillis() - DELTA*NCANDLES);
		Date next = new Date(start.getTime() + DELTA);

		double lastClose = Double.NaN;
		for (int i = 0; i < 50; i++) {
			double high = 0;
			double low = Double.MAX_VALUE;
			double close = lastClose;
			double open = lastClose;
			double volume = 0;
			if(!Double.isNaN(lastClose))
				high = lastClose;

			for (int row = maxLines-1; row >= 0; row--) {
				ContractTrade tr = trades.get(row);
				Date date = tr.getTimestamp().getAsDate();
				double price = (double)tr.getRate() / market.getFactor();

				if(date.after(start) && date.before(next)) {
					close = price;
					open = lastClose;
					high = Math.max(price, high);
					low = Math.min(price, low);
					if(Double.isNaN(open))
						open = lastClose = price;
					volume += (double)tr.getAmount() * tr.getRate() / market.getFactor();
				}
			}
			low = Math.min(high, low);
			if(!Double.isNaN(close))
				lastClose = close;

			if(!Double.isNaN(open)) {
				OHLCDataItem item = new OHLCDataItem(
						start, open, high, low, close, volume);
				data.add(item);
			}

			start = next;
			next = new Date(start.getTime() + DELTA);
		}

		DefaultOHLCDataset dataset = new DefaultOHLCDataset(market.toString(), data.toArray(new OHLCDataItem[data.size()]));

		chart.getXYPlot().setDataset(dataset);
	}
}
