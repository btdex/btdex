package btdex.ui.orderbook;

import btdex.locale.Translation;

import javax.swing.table.DefaultTableModel;

import static btdex.locale.Translation.tr;

class TableModelToken extends DefaultTableModel {
    private static final long serialVersionUID = 1L;

    private final TokenMarketPanel orderBook;
    private int COLS[];

    public TableModelToken(TokenMarketPanel orderBook, int[] cols) {
        this.orderBook = orderBook;
        this.COLS = cols;
    }

    public int getColumnCount() {
        return OrderBookSettings.columnNames.length;
    }

    public String getColumnName(int col) {
        boolean isToken = orderBook.getMarket().getTokenID()!=null;

        String colName = OrderBookSettings.columnNames[COLS[col]];
        if(col == COLS[OrderBookSettings.COL_PRICE])
            colName = tr("book_price", isToken ? "BURST" : orderBook.getMarket());
        else if(col == COLS[OrderBookSettings.COL_TOTAL])
            colName = tr("book_total", isToken ? "BURST" : orderBook.getMarket());
        else if(col == COLS[OrderBookSettings.COL_SIZE]) {
            if(isToken)
                colName = Translation.tr("book_size", orderBook.getMarket());
            else
                colName = tr("book_size", "BURST") + " (" + tr("book_deposit") + ")";
        }
        else if((col == COLS[OrderBookSettings.COL_CONTRACT]) && isToken)
            colName = tr("book_order");
        else
            colName = tr(colName);
        return colName;
    }

    public boolean isCellEditable(int row, int col) {
        return col == COLS[OrderBookSettings.COL_CONTRACT] || col == COLS[OrderBookSettings.COL_PRICE];
    }
}
