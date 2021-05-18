package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketOK extends MarketCrypto {

	static final String REGEX = "^P[0-9a-zA-Z]{33}$";
	
	public String getTicker() {
		return "OK";
	}
	
	@Override
	public long getID() {
		return MARKET_OK;
	}
    
    public int getUCA_ID() {
		return 760;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}
}