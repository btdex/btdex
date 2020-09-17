package btdex.markets;

import java.util.HashMap;

import btdex.locale.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MarketARRR extends MarketCrypto {
	private static Logger logger = LogManager.getLogger();
	public String getTicker() {
		return "ARRR";
	}

	@Override
	public long getID() {
		return MARKET_ARRR;
	}

	@Override
	public int getUCA_ID() {
		return 3951;
	}

	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		super.validate(fields);

		String addr = fields.get(ADDRESS);

		if(addr.startsWith("zs1")) {
			try {
				Bech32.decode(addr);
			}
			catch (Exception e) {
				logger.error(e.getLocalizedMessage());
				throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
			}
		} else {
			logger.error("Address must start zs1... You entered "+ addr);
			throw new Exception(Translation.tr("mkt_invalid_address", addr, toString()));
		}
	}
}
