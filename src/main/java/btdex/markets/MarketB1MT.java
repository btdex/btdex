package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketB1MT extends MarketCrypto {
	
	static final String REGEX = "^0x[0-9a-fA-F]{40}$";

	public String getTicker() {
		return "B1MT";
	}
	
	@Override
	public long getID() {
		return MARKET_B1MT;
	}

	@Override
	public int getUCA_ID() {
		return 4222;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		// TODO: add the checksum code when possible
		if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}	
}
