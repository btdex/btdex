package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;

public class MarketARRR extends MarketCrypto {
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

		if(addr.startsWith("zs1")) {
			try {
				Bech32.decode(addr);
			}
			catch (Exception e) {
				throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
			}
		}
		throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}
}
