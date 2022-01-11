package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketBHD extends MarketCrypto {
	
	public static final String TICKER = "BHD";

	public String getTicker() {
		return TICKER;
	}

	@Override
	public String getChainDetails() {
		return "BitcoinHD native chain";
	}

	@Override
	public String getExplorerLink() {
		return "https://www.btchd.org/explorer/";
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

		if( !(addr.startsWith("3") && BTCAddrValidator.validate(addr)) ) {
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
		}
	}
}
