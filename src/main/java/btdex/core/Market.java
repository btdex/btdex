package btdex.core;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public abstract class Market {
	
	public abstract long getID();
	
	private static final NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
	static {
		NF.setMinimumFractionDigits(8);
		NF.setMaximumFractionDigits(8);
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
