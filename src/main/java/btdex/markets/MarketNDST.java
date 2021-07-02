package btdex.markets;

import btdex.core.Globals;
import signumj.entity.SignumID;

public class MarketNDST extends MarketTRT {
	
	public String toString() {
		return "NDST";
	}
	
	@Override
	public SignumID getTokenID() {
		if(Globals.getInstance().isTestnet())
			return SignumID.fromLong("13826409704036227858");
		
		return SignumID.fromLong("");
	}	
}
