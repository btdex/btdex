package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;

public class MarketXCH extends MarketCrypto {
	
	public String getTicker() {
		return "XCH";
	}
	
	@Override
	public long getID() {
		return MARKET_XCH;
	}
	
	@Override
	public int getUCA_ID() {
		// TODO: Fix this when they get an actual UCA ID
		return 666666;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		// TODO: add a better check when there is some documentation
		if(!addr.startsWith(Globals.getInstance().isTestnet() ? "txch1" : "xch1")) {
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
		}
	}
}
