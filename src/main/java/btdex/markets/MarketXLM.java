package btdex.markets;

import java.util.HashMap;

import org.stellar.sdk.Account;

import btdex.locale.Translation;

public class MarketXLM extends MarketCrypto {
	
	public static final String TICKER = "XLM";

	public String getTicker() {
		return TICKER;
	}
	
	@Override
	public String getChainDetails() {
		return "Stellar chain livenet";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://stellarchain.io/";
	}
	
	@Override
	public long getID() {
		return MARKET_XLM;
	}

	@Override
	public int getUCA_ID() {
		return 512;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		try {
			Account ac = new Account(addr, 1L);
			ac.getKeyPair();
		}
		catch (Exception e) {
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
		}
	}	
}
