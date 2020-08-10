package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;

public class MarketBTC extends MarketCrypto {
	
	public String getTicker() {
		return "BTC";
	}
	
	@Override
	public long getID() {
		return MARKET_BTC;
	}
	
	@Override
	public int getUCA_ID() {
		return 1;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(addr.startsWith(Globals.getInstance().isTestnet() ? "tc1" : "bc1")) {
			try {
				Bech32.decode(addr);
			}
			catch (Exception e) {
				throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
			}
		}
		else if(!BTCAddrValidator.validate(addr)) {
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
		}
	}
}
