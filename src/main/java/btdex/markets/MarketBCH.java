package btdex.markets;

import java.util.HashMap;

import com.github.kiulian.converter.AddressConverter;

import btdex.locale.Translation;

public class MarketBCH extends MarketBTC {
	
	public String getTicker() {
		return "BCH";
	}
	
	@Override
	public long getID() {
		return MARKET_BCH;
	}
	
	@Override
	public int getUCA_ID() {
		return 1831;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		String addr = fields.get(ADDRESS);
		try {
			if(addr.startsWith("bitcoincash:")) {
				addr = AddressConverter.toLegacyAddress(addr);
			}
			if(!BTCAddrValidator.validate(addr)) {
				throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
			}
		}
		catch (Exception e) {
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
		}
	}
}
