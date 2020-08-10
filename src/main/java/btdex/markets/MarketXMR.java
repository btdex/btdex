package btdex.markets;

import java.util.HashMap;

import btdex.core.Globals;
import btdex.locale.Translation;
import monero.daemon.model.MoneroNetworkType;
import monero.utils.MoneroException;
import monero.utils.MoneroUtils;

public class MarketXMR extends MarketCrypto {
	
	public String getTicker() {
		return "XMR";
	}
	
	@Override
	public long getID() {
		return MARKET_XMR;
	}

	@Override
	public int getUCA_ID() {
		return 328;
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
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
		}
	}
}
