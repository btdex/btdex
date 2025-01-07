package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;

public class MarketBHD extends MarketCrypto {
	
	public String getTicker() {
		return "BHD";
	}
	
	@Override
	public long getID() {
		return MARKET_BHD;
	}

	@Override
	public int getUCA_ID() {
		return 3966;
	}

	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(addr.startsWith(Globals.getInstance().isTestnet() ? "thd1" : "bhd1")) {
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
