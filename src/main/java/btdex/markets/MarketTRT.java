package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstID;

public class MarketTRT extends Market {
	
	public String getTicker() {
		return "TRT";
	}
	
	@Override
	public String toString() {
		return // Constants.BURST_TICKER + "-" + 
				getTicker();
	}
	
	@Override
	public BurstID getTokenID() {
		if(Globals.getInstance().isTestnet())
			return BurstID.fromLong("13868324881938171674");
		
		return BurstID.fromLong("12402415494995249540");
	}
	
	@Override
	public NumberFormatting getNumberFormat() {
		return NumberFormatting.TOKEN;
	}
	
	@Override
	public long getFactor() {
		return 10000L;
	}
	
	public String format(long value) {
		double dvalue = (double)value/getFactor();
		return getNumberFormat().format(dvalue);
	}
	
	@Override
	public long getID() {
		// Tokens do not use the market ID, but the token ID
		return 0;
	}
	
	@Override
	public int getUCA_ID() {
		return 0;
	}
	
	@Override
	public ArrayList<String> getFieldKeys(){
		return null;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		// not needed for a token
	}
	
	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		return getTokenID().getID();
	}
}
