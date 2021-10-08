package btdex.markets;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import btdex.locale.Translation;

public class MarketXLA extends MarketCrypto {
	
	public String getTicker() {
		return "XLA";
	}
	
	@Override
	public String getChainDetails() {
		return "Scala native chain";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://explorer.scalaproject.io/";
	}
	
	@Override
	public long getID() {
		return MARKET_XLA;
	}

	@Override
	public int getUCA_ID() {
		return 2629; /* Taken from CMC */
	}

	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);

        Pattern pattern = Pattern.compile("^S[0-9A-Za-z][1-9A-HJ-NP-Za-km-z]{95}$");
		Matcher match = pattern.matcher(addr);
        boolean matchFound = match.find();

        if(!matchFound) {
            throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
        }
	}
}
