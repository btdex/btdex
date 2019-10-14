package btdex.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import burst.kit.entity.BurstID;

public abstract class Market {
	
	public static long MARKET_BTC            = 0x000000001;
	public static long MARKET_LTC            = 0x000000002;
	public static long MARKET_ETH            = 0x000000003;

	// TODO: fill with other cryptos here
	
	public static long MARKET_USD            = 0x000001000;
	public static long MARKET_EUR            = 0x000002000;
	public static long MARKET_BRL            = 0x000003000;
	// TODO: fill with other fiat currencies here
	
	public static long MARKET_MASK           = 0x0000fffff;
	
	public static long TRANSFER_SAME_BANK    = 0x000100000;
	public static long TRANSFER_NATIONAL_BANK= 0x000200000;
	public static long TRANSFER_SEPA         = 0x000300000;
	public static long TRANSFER_SEPA_INST    = 0x000400000;
	public static long TRANSFER_ZELLE        = 0x000500000;
	// TODO: fill with other FIAT transfer methods
	public static long TRANSFER_MASK         = 0x00ff00000;

	
	/**
	 * If this market is for a BURST token, should return its ID.
	 * 
	 * @return the BURST token ID or null if not a token.
	 */
	public BurstID getTokenID() {
		return null;
	}

	/**
	 * @return a unique ID for the market or 0 if is a BURST token.
	 */
	public abstract long getID();
	
	private static final NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
	static {
		NF.setMinimumFractionDigits(8);
		NF.setMaximumFractionDigits(8);
		DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
		s.setGroupingSeparator('\'');
		((DecimalFormat)NF).setDecimalFormatSymbols(s);
	}
	
	/**
	 * @return the formatted value (default is satoshi units)
	 */
	public String numberFormat(long value) {
		double dvalue = value/100000000.0;
		return NF.format(dvalue);
	}
	
	/**
	 * @return the expected field names when selling on this market.
	 */
	public abstract ArrayList<String> getFieldNames();
	
	/**
	 * Should validate the given field values, throwing an exception if invalid
	 * 
	 * @param fields
	 * @throws Exception
	 */
	public abstract void validate(HashMap<String, String> fields) throws Exception;
}
