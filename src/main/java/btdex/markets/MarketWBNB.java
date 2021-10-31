package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketWBNB extends MarketCrypto {
	
	static final String REGEX = "^0x[0-9a-fA-F]{40}$";
	
	public static final String TICKER = "WBNB";

	public String getTicker() {
		return TICKER;
	}
	
	public long getDefaultMinOffer() {
		return 100000;
	}
	
	@Override
	public String getChainDetails() {
		return "WBNB on BSC chain";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://bscscan.com/address/0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c";
	}
	
	@Override
	public long getID() {
		return MARKET_WBNB;
	}

	@Override
	public int getUCA_ID() {
		return 0;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}	
}
