package btdex.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
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
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;

import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Trade;
import burst.kit.entity.response.http.BRSError;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class HistoryPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	Icon copyIcon;
	
	Market market = null;
	private JFreeChart chart;

	public static final int COL_PRICE = 0;
	public static final int COL_AMOUNT = 1;
	public static final int COL_DATE = 2;
	public static final int COL_TYPE = 3;
	public static final int COL_ACCOUNT = 4;

	String[] columnNames = {
			"PRICE",
			"AMOUNT",
			"DATE",
			"TYPE",
			"CONTRACT",
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
//			return col == COL_ID || col == COL_ACCOUNT;
			return false;
		}
	}

	public HistoryPanel(Market market) {
		super(new BorderLayout());

		table = new JTable(model = new MyTableModel());
		table.setRowHeight(table.getRowHeight()+7);
		
		this.market = market;
		
		copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 12, table.getForeground());
		
		chart = ChartFactory.createCandlestickChart(null, null, null, null, true);
        chart.getXYPlot().setOrientation(PlotOrientation.VERTICAL);
        chart.removeLegend();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(200, 200));

        chart.setBackgroundPaint(table.getBackground());
        chart.setBorderPaint(table.getForeground());
        chart.getXYPlot().getRangeAxis().setTickLabelPaint(table.getForeground());
        chart.getXYPlot().getRangeAxis().setLabelPaint(table.getForeground());
        chart.getXYPlot().getDomainAxis().setTickLabelPaint(table.getForeground());
        chart.getXYPlot().getDomainAxis().setLabelPaint(table.getForeground());
        
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

//		table.getColumnModel().getColumn(COL_ID).setCellRenderer(OrderBook.BUTTON_RENDERER);
//		table.getColumnModel().getColumn(COL_ID).setCellEditor(OrderBook.BUTTON_EDITOR);
//		table.getColumnModel().getColumn(COL_ACCOUNT).setCellRenderer(OrderBook.BUTTON_RENDERER);
//		table.getColumnModel().getColumn(COL_ACCOUNT).setCellEditor(OrderBook.BUTTON_EDITOR);
		//
		table.getColumnModel().getColumn(COL_ACCOUNT).setPreferredWidth(200);

		add(scrollPane, BorderLayout.CENTER);
		add(chartPanel, BorderLayout.PAGE_END);
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

	public void update() {
		Globals g = Globals.getInstance();
		
		boolean isToken = market.getTokenID()!=null;
		
		try {
			if(isToken) {
				Trade trs[] = g.getNS().getAssetTrades(market.getTokenID()).blockingGet();
				
				int maxLines = Math.min(trs.length, 200);

				model.setRowCount(maxLines);

				// Update the contents
				for (int row = 0; row < maxLines; row++) {
					Trade tr = trs[row];
					BurstAddress account = null;
					long amount = tr.getQuantity().longValue();
					long price = tr.getPrice().longValue();

					model.setValueAt(account==null ? new JLabel() :
						new CopyToClipboardButton(account.getRawAddress(), copyIcon, account.getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);
					model.setValueAt(new CopyToClipboardButton(tr.getAskOrder().toString(), copyIcon, OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);

					model.setValueAt(ContractState.format(price*market.getFactor()), row, COL_PRICE);
					model.setValueAt(market.format(amount), row, COL_AMOUNT);
					model.setValueAt(tr.getTimestamp().getAsDate(), row, COL_DATE);
				}
				
				
				ArrayList<OHLCDataItem> data = new ArrayList<>();
				int NCANDLES = 50;
				long DELTA = TimeUnit.DAYS.toMillis(1);
				Date start = new Date(System.currentTimeMillis() - DELTA*NCANDLES);
				Date next = new Date(start.getTime() + DELTA);
				
				int startRow = 0;
				double lastClose = Double.NaN;
				for (int i = 0; i < 50; i++) {
					double high = 0;
					double low = Double.MAX_VALUE;
					double close = lastClose;
					double open = lastClose;
					double volume = 0;
					if(!Double.isNaN(lastClose))
						high = lastClose;
					
					for (int row = startRow; row < maxLines; row++) {
						Trade tr = trs[row];
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
