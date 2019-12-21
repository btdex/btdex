package btdex.markets;

import btdex.core.Globals;
import burst.kit.entity.BurstID;

public class MarketNDST extends MarketTRT {
	
	public String toString() {
		return "NDST";
	}
	
	@Override
	public BurstID getTokenID() {
		if(Globals.getInstance().isTestnet())
			return BurstID.fromLong("13826409704036227858");
		
		return BurstID.fromLong("");
	}	
}
