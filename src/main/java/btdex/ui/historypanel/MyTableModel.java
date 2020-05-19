package btdex.ui.historypanel;

import javax.swing.table.DefaultTableModel;

import static btdex.locale.Translation.tr;

class MyTableModel extends DefaultTableModel {
    private static final long serialVersionUID = 1L;

    private HistoryPanel historyPanel;

    public MyTableModel(HistoryPanel historyPanel) {
        this.historyPanel = historyPanel;
    }

    public int getColumnCount() {
        return HistoryPanelSettings.columnNames.length;
    }

    public String getColumnName(int col) {
        String colName = HistoryPanelSettings.columnNames[col];
        if(historyPanel.getMarket() == null)
            return colName;

        boolean isToken = historyPanel.getMarket().getTokenID()!=null;
        if(col == HistoryPanelSettings.COL_PRICE)
            colName = tr(colName, isToken ? "BURST" : historyPanel.getMarket());
        else if(col == HistoryPanelSettings.COL_AMOUNT)
            colName = tr(colName, isToken ? historyPanel.getMarket() : "BURST");
        else if(col == HistoryPanelSettings.COL_CONTRACT)
            colName = tr(isToken ? "book_order" : colName);
        else
            colName = tr(colName);
        return colName;
    }

    public boolean isCellEditable(int row, int col) {
        return col == HistoryPanelSettings.COL_CONTRACT || col == HistoryPanelSettings.COL_BUYER || col == HistoryPanelSettings.COL_SELLER;
    }
}
