package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketETH extends MarketCrypto {
	
	static final String REGEX = "^0x[0-9a-fA-F]{40}$";

	public String getTicker() {
		return "ETH";
	}
	
	@Override
	public long getID() {
		return MARKET_ETH;
	}

	@Override
	public int getUCA_ID() {
		return 1027;
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
