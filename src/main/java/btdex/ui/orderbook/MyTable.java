package btdex.ui.orderbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

class MyTable extends JTable {
    private static final long serialVersionUID = 3251005544025726619L;

    int COLS[];

    public MyTable(DefaultTableModel model, int cols[]) {
        super(model);
        this.COLS = cols;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int col) {
        if(col == COLS[OrderBookSettings.COL_CONTRACT] || col == COLS[OrderBookSettings.COL_PRICE])
            return OrderBook.BUTTON_RENDERER;

        return super.getCellRenderer(row, col);
    }

    @Override
    public TableCellEditor getCellEditor(int row, int col) {
        if(col == COLS[OrderBookSettings.COL_CONTRACT] || col == COLS[OrderBookSettings.COL_PRICE])
            return OrderBook.BUTTON_EDITOR;

        return super.getCellEditor(row, col);
    }
}
