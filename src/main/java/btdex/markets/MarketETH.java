package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Market;

public class MarketETH extends Market {
	
	static final String ADDRESS = "Address";
	static final String REGEX = "^0x[0-9a-f]{40}$";

	public String toString() {
		return "ETH";
	}
	
	@Override
	public long getID() {
		return MARKET_ETH;
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
		
		if(!addr.matches(REGEX))
			throw new Exception(addr + " is not a valid ETH address");
	}
	
	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		return fields.get(ADDRESS);
	}
}
