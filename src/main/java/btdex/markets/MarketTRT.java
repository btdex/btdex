package btdex.markets;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import btdex.core.Globals;
import btdex.core.Market;
import burst.kit.entity.BurstID;

public class MarketTRT extends Market {
	
	public String toString() {
		return "TRT";
	}
	
	@Override
	public BurstID getTokenID() {
		if(Globals.getInstance().isTestnet())
			return BurstID.fromLong("13868324881938171674");
		
		return BurstID.fromLong("107507544026763809");
	}
	
	private static final NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
	static {
		NF.setMinimumFractionDigits(4);
		NF.setMaximumFractionDigits(4);
		
		DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
		s.setGroupingSeparator('\'');
		((DecimalFormat)NF).setDecimalFormatSymbols(s);
	}
	
	@Override
	public NumberFormat getNumberFormat() {
		return NF;
	}
	
	@Override
	public long getFactor() {
		return 10000L;
	}

	
	public String format(long value) {
		double dvalue = (double)value/getFactor();
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
	
	@Override
	public String format(HashMap<String, String> fields) throws Exception {
		// not needed for a token
		return "";
	}
}
