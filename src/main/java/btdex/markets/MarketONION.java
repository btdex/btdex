package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;

public class MarketONION extends MarketCrypto {
	
	static final String REGEX = "^[D][a-km-zA-HJ-NP-Z1-9]{26,33}$";

	public String getTicker() {
		return "ONION";
	}
	
	@Override
	public long getID() {
		return MARKET_ONION;
	}
	
	@Override
	public int getUCA_ID() {
		return 1881;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(addr.startsWith(Globals.getInstance().isTestnet() ? "tdpn1" : "dpn1")) {
			try {
				Bech32.decode(addr);
			}
			catch (Exception e) {
				throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
			}
		}
		else if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}
}
