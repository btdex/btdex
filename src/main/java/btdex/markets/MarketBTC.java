package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Market;

public class MarketBTC extends Market {
	
	static final String ADDRESS = "Address";

	public String toString() {
		return "BTC";
	}
	
	@Override
	public long getID() {
		return MARKET_BTC;
	}
	
	@Override
	public ArrayList<String> getFieldNames(){
		ArrayList<String> fieldNames = new ArrayList<>();
		fieldNames.add(ADDRESS);
		return fieldNames;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		String addr = fields.get(ADDRESS);
		
		if(!BTCAddrValidator.validate(addr))
			throw new Exception(addr + " is not a valid BTC address");
	}
}
