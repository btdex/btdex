package btdex.markets;

import java.util.HashMap;

import btdex.core.NumberFormatting;
import btdex.locale.Translation;

public class MarketDOGE extends MarketCrypto {
	
	public String getTicker() {
		return "DOGE";
	}
	
	@Override
	public long getID() {
		return MARKET_DOGE;
	}
	
	@Override
	public int getUCA_ID() {
		return 74;
	}
	
	@Override
	public NumberFormatting getNumberFormat() {
		return NumberFormatting.BURST;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(!BTCAddrValidator.validate(addr, BTCAddrValidator.DOGE_HEADERS))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}
}
