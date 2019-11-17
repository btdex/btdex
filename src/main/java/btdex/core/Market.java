package btdex.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import com.google.gson.JsonObject;

import burst.kit.entity.BurstID;

public abstract class Market {
	
	public static long BTC_TO_SAT = 100000000L;
	public static long BURST_TO_PLANCK = 100000000L;
	
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
	 * @return the formatted value (assuming value is in SATs)
	 */
	public String format(long value) {
		double dvalue = (double)value/BTC_TO_SAT;
		return NF.format(dvalue);
	}
	
	public NumberFormat getNumberFormat() {
		return NF;
	}
	
	/**
	 * Factor to get amounts as a long (maximum number of decimal places).
	 * 
	 * @return the multiplying factor to get amounts on this market as a long 
	 */
	public long getFactor() {
		return BTC_TO_SAT;
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
	
	/**
	 * @param fields
	 * @return a simple formatted version of the fields
	 */
	public abstract String simpleFormat(HashMap<String, String> fields);
	
	/**
	 * Format the fields in a Json format.
	 * 
	 * @param fields
	 * @throws Exception
	 */
	public String format(HashMap<String, String> fields) {
		JsonObject params = new JsonObject();
		for (String key: fields.keySet()) {
			params.addProperty(key, fields.get(key));			
		}
		return params.toString();
	}
}
