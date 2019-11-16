package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Market;

public class MarketLTC extends Market {
	
	static final String ADDRESS = "Address";
	static final String REGEX = "^L[a-km-zA-HJ-NP-Z1-9]{26,33}$";

	public String toString() {
		return "LTC";
	}
	
	@Override
	public long getID() {
		return MARKET_LTC;
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
			throw new Exception(addr + " is not a valid LTC address");
	}

	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		return fields.get(ADDRESS);
	}
}
