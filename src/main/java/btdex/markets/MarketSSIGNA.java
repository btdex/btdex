package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;

public class MarketSSIGNA extends MarketCrypto {

	public String getTicker() {
		return "SWAP.SIGNA";
	}

	@Override
	public String getChainDetails() {
		return "SWAP.SIGNA ON HIVE ENGINE";
	}
	
	@Override
	public String getExplorerLink() {
		return "https://he.dtools.dev";
	}
	
	@Override
	public long getID() {
		return MARKET_SSIGNA;
	}
	
	@Override
	public int getUCA_ID() {
		return 10776;
	}
}
