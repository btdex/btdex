package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketHIVE extends MarketCrypto {
	
	static final String REGEX = "^@[a-z][a-z0-9\\-]{1,14}[a-z0-9]$";

	public String getTicker() {
		return "HIVE";
	}

	@Override
	public String getChainDetails() {
		return "Hive Engine Native Chain";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://he.dtools.dev";
	}
	
	@Override
	public long getID() {
		return MARKET_HIVE;
	}
	
	@Override
	public int getUCA_ID() {
		return 5370;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}	
}
