package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Globals;
import btdex.core.Market;
import monero.daemon.model.MoneroNetworkType;
import monero.utils.MoneroException;
import monero.utils.MoneroUtils;

public class MarketXMR extends Market {
	
	static final String ADDRESS = "Address";

	public String toString() {
		return "XMR";
	}
	
	@Override
	public long getID() {
		return MARKET_XMR;
	}
	
	@Override
	public ArrayList<String> getFieldNames(){
		ArrayList<String> fieldNames = new ArrayList<>();
		fieldNames.add(ADDRESS);
		return fieldNames;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
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

	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		return fields.get(ADDRESS);
	}
}
