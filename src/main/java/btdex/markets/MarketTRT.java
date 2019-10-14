package btdex.markets;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import btdex.core.Market;
import burst.kit.entity.BurstID;

public class MarketTRT extends Market {
	
	public String toString() {
		return "TRT";
	}
	
	@Override
	public BurstID getTokenID() {
		return BurstID.fromLong("6124399134335154227");
	}
	
	private static final NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
	static {
		NF.setMinimumFractionDigits(4);
		NF.setMaximumFractionDigits(4);
		
		DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
		s.setGroupingSeparator('\'');
		((DecimalFormat)NF).setDecimalFormatSymbols(s);
	}

	
	public String numberFormat(long value) {
		double dvalue = value/10000.0;
		return NF.format(dvalue);
	}
	
	@Override
	public long getID() {
		// Tokens do not use the market ID, but the token ID
		return 0;
	}
	
	@Override
	public ArrayList<String> getFieldNames(){
		return null;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		// not needed for a token
	}
}
