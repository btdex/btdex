package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Market;

public class MarketBTDEX extends Market {
	
	public String toString() {
		return "BTDEX";
	}
	
	@Override
	public String getBurstTokenID() {
		return "15164063889875975413";
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
