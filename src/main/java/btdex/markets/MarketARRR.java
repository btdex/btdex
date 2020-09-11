package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketARRR extends MarketCrypto {
	
	static final String REGEX = "^R[a-zA-Z0-9]{33}$";

	public String getTicker() {
		return "ARRR";
	}
	
	@Override
	public long getID() {
		return MARKET_ARRR;
	}

	@Override
	public int getUCA_ID() {
		return 3951;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}	
}
