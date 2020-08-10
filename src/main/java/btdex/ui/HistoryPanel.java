package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
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
import btdex.core.Constants;
import btdex.core.ContractTrade;
import btdex.core.ContractType;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import btdex.ui.orderbook.BookTable;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.AssetTrade;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class HistoryPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon, expIcon, upIcon, downIcon;
	
	private JToggleButton timeButtons[];

	private static final int NCANDLES = 80;

	public static final Color RED = Color.decode("#BE474A");
	public static final Color GREEN = Color.decode("#29BF76");

	Market market = null, newMarket;

	private JFreeChart chart;

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");

	public static final int COL_PRICE = 0;
	public static final int COL_AMOUNT = 1;
	public static final int COL_TIME = 2;
	public static final int COL_CONTRACT = 3;
	public static final int COL_BUYER = 4;
	public static final int COL_SELLER = 5;

	String[] columnNames = {
			"book_price",
			"book_size",
			"hist_time",
			"book_contract",
			"hist_buyer",
			"hist_seller",
	};

	private ExplorerButton tokenIdButton;

	private Desc lastPrice;

	private boolean onlyMine;

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
			else if(col == COL_CONTRACT)
				colName = tr(isToken ? "book_order" : colName);				
			else
				colName = tr(colName);
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_CONTRACT || col == COL_BUYER || col == COL_SELLER;
		}
	}

	public HistoryPanel(Main main, Market market, Desc lastPrice) {
		super(new BorderLayout());

		this.lastPrice = lastPrice;
		
		JPanel top = new JPanel(new BorderLayout());

		table = new JTable(model = new MyTableModel());
		table.setRowSelectionAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowHeight(table.getRowHeight()+10);
		table.setPreferredScrollableViewportSize(new Dimension(200, 120));

		copyIcon = IconFontSwing.buildIcon(Icons.COPY, Constants.ICON_SIZE_SMALL, table.getForeground());
		expIcon = IconFontSwing.buildIcon(Icons.EXPLORER, Constants.ICON_SIZE_SMALL, table.getForeground());
		
		upIcon = IconFontSwing.buildIcon(Icons.UP, Constants.ICON_SIZE, HistoryPanel.GREEN);
		downIcon = IconFontSwing.buildIcon(Icons.DOWN, Constants.ICON_SIZE, HistoryPanel.RED);
		
		tokenIdButton = new ExplorerButton("", IconFontSwing.buildIcon(Icons.COPY, Constants.ICON_SIZE_MED, table.getForeground()),
				IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, Constants.ICON_SIZE_MED, table.getForeground()));
		tokenIdButton.setVisible(false);
		
		timeButtons = new JToggleButton[4];
		timeButtons[0] = new JToggleButton(tr("hist_1hour"));
		timeButtons[1] = new JToggleButton(tr("hist_4hours"));
		timeButtons[2] = new JToggleButton(tr("hist_1day"));
		timeButtons[3] = new JToggleButton(tr("hist_1week"));
		
		setMarket(market);
		
		JPanel chartPanel = new JPanel(new BorderLayout(0,0));
		JPanel chartTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		ButtonGroup buttonGroup = new ButtonGroup();
		for (int i = 0; i < timeButtons.length; i++) {
			chartTopPanel.add(timeButtons[i]);
			buttonGroup.add(timeButtons[i]);
			timeButtons[i].addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					update();
					// TODO: this theme is now showing the selected button, so this little trick
					for (int j = 0; j < timeButtons.length; j++) {
						timeButtons[j].setForeground(timeButtons[j].isSelected() ?
								Color.WHITE : chartPanel.getForeground());
					}
				}
			});
		}
		timeButtons[2].setForeground(Color.WHITE); // TODO: same here
		buttonGroup.setSelected(timeButtons[2].getModel(), true);
		
		chartPanel.add(chartTopPanel, BorderLayout.PAGE_START);
		ChartPanel chartPanelChart = null;
		chart = ChartFactory.createCandlestickChart(null, null, null, null, true);
		chart.getXYPlot().setOrientation(PlotOrientation.VERTICAL);
		chart.removeLegend();
		chartPanelChart = new ChartPanel(chart);
		chartPanelChart.setPreferredSize(new java.awt.Dimension(200, 200));
		chartPanel.add(chartPanelChart, BorderLayout.CENTER);

		chart.setBackgroundPaint(table.getBackground());
		chart.setBorderPaint(table.getForeground());
		chart.getXYPlot().setBackgroundPaint(table.getBackground());
		if (Locale.getDefault().equals(Locale.SIMPLIFIED_CHINESE))
			chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font("微软雅黑", Font.PLAIN, 15));
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

		table.getColumnModel().getColumn(COL_CONTRACT).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_CONTRACT).setCellEditor(BookTable.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_BUYER).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_BUYER).setCellEditor(BookTable.BUTTON_EDITOR);
		table.getColumnModel().getColumn(COL_SELLER).setCellRenderer(BookTable.BUTTON_RENDERER);
		table.getColumnModel().getColumn(COL_SELLER).setCellEditor(BookTable.BUTTON_EDITOR);
		//
		table.getColumnModel().getColumn(COL_TIME).setPreferredWidth(120);
		table.getColumnModel().getColumn(COL_CONTRACT).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_BUYER).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_SELLER).setPreferredWidth(200);

		add(top, BorderLayout.PAGE_START);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, scrollPane);		
		add(splitPane, BorderLayout.CENTER);
	}

	public void setMarket(Market m) {
		newMarket = m;
		tokenIdButton.setVisible(m.getTokenID()!=null);
		if(m.getTokenID()!=null) {
			tokenIdButton.getMainButton().setText(tr("main_token_id", m.toString(), m.getTokenID().getID()));
			tokenIdButton.setTokenID(m.getTokenID().getID());
		}
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
	
	public void setFilter(boolean onlyMine) {
		this.onlyMine  = onlyMine;
	}

	private void updateToken() {
		Globals g = Globals.getInstance();
		BurstNode bn = BurstNode.getInstance();

		AssetTrade trs[] = bn.getAssetTrades(market);
		
		AssetTrade lastTrade = trs !=null && trs.length > 0 ? trs[0] : null;
		boolean lastIsUp = true;
		if(trs !=null && trs.length > 1 && trs[0].getPrice().longValue() < trs[1].getPrice().longValue())
			lastIsUp = false;

		if(lastTrade != null) {
			// set the last price label
			String priceText = NumberFormatting.BURST.format(lastTrade.getPrice().longValue()*market.getFactor()) + " BURST";
			JLabel priceLabel = (JLabel) lastPrice.getChild();
			lastPrice.setVisible(true);
			priceLabel.setText(priceText);
			priceLabel.setIcon(lastIsUp ? upIcon : downIcon);
			priceLabel.setForeground(lastIsUp ? HistoryPanel.GREEN : HistoryPanel.RED);
		}
		else {
			lastPrice.setVisible(false);
		}

		if(trs == null) {
			model.setRowCount(0);
			chart.getXYPlot().setDataset(null);
			return;
		}

		int nLines = 0;

		int maxLines = Math.min(200, trs == null ? 0 : trs.length);
		for (int i = 0; i < maxLines; i++) {
			AssetTrade tr = trs[i];
			if(onlyMine &&
					tr.getBuyerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId() &&
					tr.getSellerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId())
				continue;
			nLines++;
		}

		model.setRowCount(nLines);

		// Update the contents
		for (int row = 0, i=0; i < maxLines; i++) {
			AssetTrade tr = trs[i];
			if(onlyMine &&
					tr.getBuyerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId() &&
					tr.getSellerAddress().getBurstID().getSignedLongId()!=g.getAddress().getSignedLongId())
				continue;

			long amount = tr.getQuantity().longValue();
			long price = tr.getPrice().longValue();
			
			model.setValueAt(new ExplorerButton(
					tr.getBuyerAddress().equals(g.getAddress()) ? tr("hist_you") : tr.getBuyerAddress().getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, tr.getBuyerAddress().getID(), tr.getBuyerAddress().getFullAddress()), row, COL_BUYER);
			model.setValueAt(new ExplorerButton(
					tr.getSellerAddress().equals(g.getAddress()) ? tr("hist_you") : tr.getSellerAddress().getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, tr.getSellerAddress().getID(), tr.getSellerAddress().getFullAddress()), row, COL_SELLER);
			
			// TODO: check if ask or bid was more recent to add one here (missing burstkit4j function for this)
			model.setValueAt(new ExplorerButton(tr.getAskOrderId().getID(), copyIcon, expIcon), row, COL_CONTRACT);

			model.setValueAt(NumberFormatting.BURST.format(price*market.getFactor()), row, COL_PRICE);
			model.setValueAt(market.format(amount), row, COL_AMOUNT);
			model.setValueAt(DATE_FORMAT.format(tr.getTimestamp().getAsDate()), row, COL_TIME);

			row++;
		}
		
		ArrayList<OHLCDataItem> data = new ArrayList<>();
		int hours = 1;
		if(timeButtons[1].isSelected())
			hours = 4;
		else if(timeButtons[2].isSelected())
			hours = 24;
		else if(timeButtons[3].isSelected())
			hours = 24*7;
		long delta = TimeUnit.HOURS.toMillis(hours);
		Date start = new Date(System.currentTimeMillis() - delta*NCANDLES);
		Date next = new Date(start.getTime() + delta);

		double lastClose = Double.NaN;
		for (int i = 0; i < NCANDLES; i++) {
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
			next = new Date(start.getTime() + delta);
		}

		DefaultOHLCDataset dataset = new DefaultOHLCDataset(market.toString(), data.toArray(new OHLCDataItem[data.size()]));

		chart.getXYPlot().setDataset(dataset);
	}

	private void updateContract() {
		Globals g = Globals.getInstance();

		ArrayList<ContractTrade> trades = new ArrayList<>();
		// build the trade list for this market
		for(ContractTrade t : Contracts.getTrades()) {
			if(t.getMarket() == market.getID()) {
				trades.add(t);
				// TODO: break if these trades are old enough
			}
		}
		
		ContractTrade lastTrade = trades.size() > 1 ? trades.get(0) : null;
		boolean lastIsUp = true;
		if(trades.size() > 1 && trades.get(0).getRate() < trades.get(1).getRate())
			lastIsUp = false;

		if(lastTrade != null) {
			// set the last price label
			String priceText = market.getNumberFormat().format(lastTrade.getRate()) + " " + market;
			JLabel priceLabel = (JLabel) lastPrice.getChild();
			lastPrice.setVisible(true);
			priceLabel.setText(priceText);
			priceLabel.setIcon(lastIsUp ? upIcon : downIcon);
			priceLabel.setForeground(lastIsUp ? HistoryPanel.GREEN : HistoryPanel.RED);
		}
		else {
			lastPrice.setVisible(false);
		}
		
		int maxLines = Math.min(200, trades.size());
		int nLines = onlyMine ? 0 : maxLines;
		for (int i = 0; onlyMine && i < maxLines; i++) {
			ContractTrade tr = trades.get(i);
			if(onlyMine && !tr.getCreator().equals(g.getAddress()) && !tr.getTaker().equals(g.getAddress()))
				continue;
			nLines++;
		}

		model.setRowCount(nLines);

		// Update the contents
		for (int row = 0, i=0; i < maxLines; i++) {
			ContractTrade tr = trades.get(i);
			if(onlyMine && !tr.getCreator().equals(g.getAddress()) && !tr.getTaker().equals(g.getAddress()))
				continue;

			long amount = tr.getAmount();
			double price = (double)tr.getRate() / market.getFactor();
			
			BurstAddress buyer = tr.getContract().getType() == ContractType.BUY ? tr.getCreator() : tr.getTaker();
			BurstAddress seller = tr.getContract().getType() == ContractType.BUY ? tr.getTaker() : tr.getCreator();

			model.setValueAt(new ExplorerButton(
					buyer.equals(g.getAddress()) ? tr("hist_you") : buyer.getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, buyer.getID(), buyer.getFullAddress()), row, COL_BUYER);
			model.setValueAt(new ExplorerButton(
					seller.equals(g.getAddress()) ? tr("hist_you") : seller.getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, seller.getID(), seller.getFullAddress()), row, COL_SELLER);

			model.setValueAt(new ExplorerButton(
					tr.getContract().getAddress().getRawAddress(), copyIcon, expIcon,
							ExplorerButton.TYPE_ADDRESS, tr.getContract().getAddress().getID(),
							tr.getContract().getAddress().getFullAddress()), row, COL_CONTRACT);

			model.setValueAt(market.getNumberFormat().format(price), row, COL_PRICE);
			model.setValueAt(NumberFormatting.BURST.format(amount), row, COL_AMOUNT);
			model.setValueAt(DATE_FORMAT.format(tr.getTimestamp().getAsDate()), row, COL_TIME);

			row++;
		}

		ArrayList<OHLCDataItem> data = new ArrayList<>();
		int hours = 1;
		if(timeButtons[1].isSelected())
			hours = 4;
		else if(timeButtons[2].isSelected())
			hours = 24;
		else if(timeButtons[3].isSelected())
			hours = 24*7;
		long DELTA = TimeUnit.HOURS.toMillis(hours);
		Date start = new Date(System.currentTimeMillis() - DELTA*NCANDLES);
		Date next = new Date(start.getTime() + DELTA);

		double lastClose = Double.NaN;
		for (int i = 0; i < NCANDLES; i++) {
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
