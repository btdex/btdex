package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;

public class MarketWSIGNA extends MarketCrypto {
	
	static final String REGEX = "^0x[0-9a-fA-F]{40}$";
	
	public static final String TICKER = "WSIGNA";

	public String getTicker() {
		return TICKER;
	}
	
	@Override
	public String getChainDetails() {
		return "SIGNA on BSC chain";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://bscscan.com/";
	}
	
	@Override
	public long getID() {
		return MARKET_WSIGNA;
	}

	@Override
	public int getUCA_ID() {
		return 10776;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		// TODO: add the checksum code when possible
		if(!addr.matches(REGEX))
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
	}	
}
