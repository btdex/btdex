package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import monero.daemon.model.MoneroNetworkType;
import monero.utils.MoneroException;
import monero.utils.MoneroUtils;

public class MarketXMR extends MarketCrypto {
	
	public String toString() {
		return "XMR";
	}
	
	@Override
	public long getID() {
		return MARKET_XMR;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);
		
		String addr = fields.get(ADDRESS);
		
		MoneroNetworkType type = Globals.getInstance().isTestnet() ?
				MoneroNetworkType.TESTNET : MoneroNetworkType.MAINNET;
		
		try{
			MoneroUtils.validateAddress(addr, type);
		}
		catch (MoneroException e) {
			throw new Exception(addr + " is not a valid XMR (Monero) address");
		}
	}
}
