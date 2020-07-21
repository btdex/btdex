package btdex.ui.orderbook;

public class OrderBookSettings {
    public static final int COL_CONTRACT = 0;
    public static final int COL_TOTAL = 1;
    public static final int COL_SIZE = 2;
    public static final int COL_PRICE = 3;
    public static final int[] BID_COLS = {0, 1, 2, 3};
    public static final int[] ASK_COLS = {3, 2, 1, 0};
    public static final int COL_WIDE = 100;
    public static final int COL_REGULAR = 75;
    public static final int ICON_SIZE = 12;
    public static final String[] columnNames = {
            "book_contract",
            "book_total",
            "book_size",
            "book_price",
    };
}
