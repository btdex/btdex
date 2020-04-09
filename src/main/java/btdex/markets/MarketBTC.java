package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketBTC extends MarketCrypto {
	
	public String toString() {
		return "BTC";
	}
	
	@Override
	public long getID() {
		return MARKET_BTC;
	}
	
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(!BTCAddrValidator.validate(addr))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}
}
