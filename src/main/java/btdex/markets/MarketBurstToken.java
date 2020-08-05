package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Market;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.Asset;
import burst.kit.service.BurstNodeService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MarketBurstToken extends Market {

	private String ticker;
	private long factor;
	private BurstID tokenID;
	private int decimals;
	private NumberFormatting NF;

	private static Logger logger = LogManager.getLogger();

	public MarketBurstToken(String id, BurstNodeService NS) {
		try {
			tokenID = BurstID.fromLong(id);
			Asset asset = NS.getAsset(tokenID).blockingGet();
			ticker = asset.getName().toUpperCase();
			decimals = asset.getDecimals();
			factor = 1L;
			for (int i = 0; i < decimals; i++) {
				factor *= 10L;
			}
			logger.debug("Burst Token ID {} , ticker {}", tokenID, ticker);
		}
		catch (Exception e) {
			logger.error("Error: {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
		this.NF = NumberFormatting.NF(Math.min(1, decimals), decimals);
	}

	public MarketBurstToken(BurstID id, String ticker, int decimals) {
		tokenID = id;
		this.ticker = ticker;
		this.decimals = decimals;
		factor = 1L;
		for (int i = 0; i < decimals; i++) {
			factor *= 10L;
		}
		logger.debug("Burst Token ID {} , ticker {}", tokenID, ticker);
		this.NF = NumberFormatting.NF(Math.min(1, decimals), decimals);
	}

	public String getTicker() {
		return ticker;
	}
	
	@Override
	public String toString() {
		return getTicker(); // Constants.BURST_TICKER + "-" + getTicker();
	}

	@Override
	public BurstID getTokenID() {
		return tokenID;
	}

	@Override
	public NumberFormatting getNumberFormat() {
		return NF;
	}

	@Override
	public long getFactor() {
		return factor;
	}

	public String format(long value) {
		double dvalue = (double)value/getFactor();
		return getNumberFormat().format(dvalue);
	}

	@Override
	public long getID() {
		// Tokens do not use the market ID, but the token ID
		return 0;
	}

	@Override
	public int getUCA_ID() {
		return 0;
	}

	@Override
	public ArrayList<String> getFieldKeys(){
		return null;
	}

	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		// not needed for a token
	}

	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		return getTokenID().getID();
	}
}
