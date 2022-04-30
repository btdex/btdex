package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;

public class MarketHIVE extends MarketCrypto {

	public String getTicker() {
		return "HIVE";
	}

	@Override
	public String getChainDetails() {
		return "Hive Engine Native Chain";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://he.dtools.dev";
	}
	
	@Override
	public long getID() {
		return MARKET_HIVE;
	}
	
	@Override
	public int getUCA_ID() {
		return 5370;
	}
}
