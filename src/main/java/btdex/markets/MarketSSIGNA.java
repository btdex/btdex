package btdex.markets;

public class MarketSSIGNA extends MarketHIVE {

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
