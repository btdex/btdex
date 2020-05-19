package btdex.ui.historypanel;

public class HistoryPanelSettings {
    public static final int COL_PRICE = 0;
    public static final int COL_AMOUNT = 1;
    public static final int COL_TIME = 2;
    public static final int COL_CONTRACT = 3;
    public static final int COL_BUYER = 4;
    public static final int COL_SELLER = 5;
    public static final int NCANDLES = 80;

    public static final int COL_TIME_WIDTH = 120;
    public static final int COL_CONTRACT_WIDTH = 200;
    public static final int COL_BUYER_WIDTH = 200;
    public static final int COL_SELLER_WIDTH = 200;

    public static final int VIEWPORT_SIZE_WIDTH = 200;
    public static final int VIEWPORT_SIZE_HEIGHT = 200;

    public static final int ICONS_SIZE = 12;
    public static final int ARROWS_SIZE = 18;

    public static final String[] columnNames = {
            "book_price",
            "book_size",
            "hist_time",
            "book_contract",
            "hist_buyer",
            "hist_seller",
    };
}
