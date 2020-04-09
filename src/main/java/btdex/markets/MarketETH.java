package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketETH extends MarketCrypto {
	
	static final String REGEX = "^0x[0-9a-f]{40}$";

	public String toString() {
		return "ETH";
	}
	
	@Override
	public long getID() {
		return MARKET_ETH;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}	
}
