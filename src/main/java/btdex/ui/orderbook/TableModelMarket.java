package btdex.ui.orderbook;

import static btdex.locale.Translation.tr;

import javax.swing.table.DefaultTableModel;

class TableModelMarket extends DefaultTableModel {
    private static final long serialVersionUID = 1L;

    private final MarketPanel orderBook;
    private int COLS[];

    public TableModelMarket(MarketPanel orderBook, int[] cols) {
        this.orderBook = orderBook;
        this.COLS = cols;
    }

    public int getColumnCount() {
        return OrderBookSettings.columnNames.length;
    }

    public String getColumnName(int col) {
        String colName = OrderBookSettings.columnNames[COLS[col]];
        if(col == COLS[OrderBookSettings.COL_PRICE])
            colName = tr("book_price", orderBook.getMarket());
        else if(col == COLS[OrderBookSettings.COL_TOTAL])
            colName = tr("book_total", orderBook.getMarket());
        else if(col == COLS[OrderBookSettings.COL_SIZE]) {
                colName = tr("book_size", "BURST") + " (" + tr("book_deposit") + ")";
        }
        else
            colName = tr(colName);
        return colName;
    }

    public boolean isCellEditable(int row, int col) {
        return col == COLS[OrderBookSettings.COL_CONTRACT] || col == COLS[OrderBookSettings.COL_PRICE];
    }
}
