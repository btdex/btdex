package btdex.markets;

import java.util.HashMap;

import bt.Contract;
import btdex.locale.Translation;

public class MarketBUSDT extends MarketCrypto {
	
	static final String REGEX = "^0x[0-9a-fA-F]{40}$";
	
	public static final String TICKER = "BUSDT";

	public String getTicker() {
		return TICKER;
	}
	
	public long getDefaultMinOffer() {
		return 10*Contract.ONE_BURST;
	}
	
	@Override
	public String getChainDetails() {
		return "USDT on BSC chain";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://bscscan.com/address/0x55d398326f99059ff775485246999027b3197955";
	}
	
	@Override
	public long getID() {
		return MARKET_BUSDT;
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
