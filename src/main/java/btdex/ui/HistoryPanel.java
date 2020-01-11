package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;

import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import burst.kit.entity.response.AssetTrade;
import burst.kit.entity.response.http.BRSError;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class HistoryPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon;
	JCheckBox listOnlyMine;
	
	public static final Color RED = Color.decode("#BE474A");
	public static final Color GREEN = Color.decode("#29BF76");
	
	Market market = null;
	private JFreeChart chart;

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");

	public static final int COL_PRICE = 0;
	public static final int COL_AMOUNT = 1;
	public static final int COL_TIME = 2;
	public static final int COL_BUYER = 3;
	public static final int COL_SELLER = 4;

	String[] columnNames = {
			"PRICE",
			"AMOUNT",
			"TIME",
			"BUYER",
			"SELLER",
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
			return col == COL_BUYER || col == COL_SELLER;
		}
	}

	public HistoryPanel(Main main, Market market) {
		super(new BorderLayout());
		
		listOnlyMine = new JCheckBox("List my trades only");
		listOnlyMine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.setRowCount(0);
				model.fireTableDataChanged();
				main.update();
			}
		});

		table = new JTable(model = new MyTableModel());
		table.setRowHeight(table.getRowHeight()+10);
		table.setPreferredScrollableViewportSize(new Dimension(200, 200));

		this.market = market;

		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());

		ChartPanel chartPanel = null;
		chart = ChartFactory.createCandlestickChart(null, null, null, null, true);
		chart.getXYPlot().setOrientation(PlotOrientation.VERTICAL);
		chart.removeLegend();
		chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(200, 200));

		chart.setBackgroundPaint(table.getBackground());
		chart.setBorderPaint(table.getForeground());
		chart.getXYPlot().setBackgroundPaint(table.getBackground());
		chart.getXYPlot().getRangeAxis().setTickLabelPaint(table.getForeground());
		chart.getXYPlot().getRangeAxis().setLabelPaint(table.getForeground());
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

		add(listOnlyMine, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
		add(chartPanel, BorderLayout.PAGE_END);
	}

	public void setMarket(Market m) {
		this.market = m;

		// update the column headers
		for (int c = 0; c < columnNames.length; c++) {
			table.getColumnModel().getColumn(c).setHeaderValue(model.getColumnName(c));
		}
		model.setRowCount(0);
		model.fireTableDataChanged();

		if(chart!=null)
			chart.getXYPlot().setDataset(null);
	}

	public void update() {
		Globals g = Globals.getInstance();

		boolean isToken = market.getTokenID()!=null;
		boolean myHistory = listOnlyMine.isSelected();

		try {
			if(isToken) {
				AssetTrade trs[] = g.getNS().getAssetTrades(market.getTokenID(),
						// myHistory ? g.getAddress() :
							null, 0, 200).blockingGet();

				int nLines = 0;

				int maxLines = Math.min(200, trs.length);
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
							tr.getBuyerAddress().getSignedLongId()==g.getAddress().getSignedLongId() ? "YOU" : tr.getBuyerAddress().getRawAddress(), copyIcon,
							ExplorerButton.TYPE_ADDRESS, tr.getBuyerAddress().getID(), tr.getBuyerAddress().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_BUYER);
					model.setValueAt(new ExplorerButton(
							tr.getSellerAddress().getSignedLongId()==g.getAddress().getSignedLongId() ? "YOU" : tr.getSellerAddress().getRawAddress(), copyIcon,
							ExplorerButton.TYPE_ADDRESS, tr.getSellerAddress().getID(), tr.getSellerAddress().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_SELLER);

					model.setValueAt(ContractState.format(price*market.getFactor()), row, COL_PRICE);
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

			else {
				if(chart!=null)
					chart.getXYPlot().setDataset(null);
			}
		}
		catch (Exception e) {
			if(e.getCause() instanceof BRSError) {
				BRSError error = (BRSError) e.getCause();
				if(error.getCode() != 5) // unknown account
					throw e;
			}
			else
				throw e;
			// Unknown account, no transactions
		}
	}
}
