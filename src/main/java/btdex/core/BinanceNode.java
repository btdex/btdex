package btdex.core;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.binance.dex.api.client.BinanceDexApiClientFactory;
import com.binance.dex.api.client.BinanceDexApiRestClient;
import com.binance.dex.api.client.BinanceDexEnvironment;
import com.binance.dex.api.client.domain.Account;
import com.binance.dex.api.client.domain.Balance;
import com.binance.dex.api.client.domain.Fees;
import com.binance.dex.api.client.domain.Infos;

import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.AssetTrade;

/**
 * Keeps updated the relevant information from a Binance chain node.
 *
 * This is intended to reduce network traffic and make the UI more responsive.
 * All blocking calls are made on a separate thread and new information
 * if requested only when a new block arrives (except for the unconfirmed
 * transactions which are frequently updated).
 *
 * TODO: create a structure of listeners, so only changes are notified
 * and we can save resources and make it more scalable.
 *
 * @author jjos
 *
 */
public class BinanceNode {

	private HashMap<Market, AssetTrade[]> assetTrades = new HashMap<>();
	private HashMap<Market, AssetOrder[]> askOrders = new HashMap<>();
	private HashMap<Market, AssetOrder[]> bidOrders = new HashMap<>();
	
	private HashMap<Market, BurstValue> baseVolume24h = new HashMap<>();
	private HashMap<Market, BurstValue> quoteVolume24h = new HashMap<>();
	private HashMap<Market, Double> priceChangePerc24h = new HashMap<>();
	private HashMap<Market, BurstValue> highestPrice24h = new HashMap<>();
	private HashMap<Market, BurstValue> lowestPrice24h = new HashMap<>();	
	
	private Exception nodeError;
	private Account account;
	private DateTime lastestBlockTime;
	
	private AtomicReference<List<Fees>> fees = new AtomicReference<>();

	private static Logger logger = LogManager.getLogger();
	
	static BinanceNode INSTANCE;

	public static BinanceNode getInstance() {
		if(INSTANCE==null)
			INSTANCE = new BinanceNode();
		return INSTANCE;
	}

	public BinanceNode() {
		try {
			// start the node updater thread
			Timer timer = new Timer("binance node update");
			timer.schedule(new NodeUpdateTask(), 0, 3000);
			logger.info("Binance chain update thread started");
		}
		catch (Exception e) {
			logger.error("Error 1: {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public long getFee(String txType) {
		for(Fees f : fees.get()) {
            if(f.getFixedFeeParams()!=null && txType.equals(f.getFixedFeeParams().getMsgType()))
                return f.getFixedFeeParams().getFee();
        }
		// error
		return -1;
	}

	public Balance getAssetBalance(String id) {
		if(account != null) {
			for(Balance b : account.getBalances()) {
				if(b.getSymbol().equals(id))
					return b;
			}
		}
		return null;
	}

	public AssetOrder[] getAssetBids(Market m) {
		return bidOrders.get(m);
	}

	public AssetOrder[] getAssetAsks(Market m) {
		return askOrders.get(m);
	}
	
	public BurstValue getBaseVolume24h(Market m) {
		return baseVolume24h.get(m);
	}

	public BurstValue getQuoteVolume24h(Market m) {
		return quoteVolume24h.get(m);
	}

	public double getPriceChangePerc24h(Market m) {
		return priceChangePerc24h.get(m);
	}
	
	public BurstValue getHighestPrice24h(Market m) {
		return highestPrice24h.get(m);
	}
	
	public BurstValue getLowestPrice24h(Market m) {
		return lowestPrice24h.get(m);
	}

	public Account getAccount() {
		return account;
	}

	public Exception getNodeException() {
		return nodeError;
	}

	public AssetTrade[] getAssetTrades(Market id){
		return assetTrades.get(id);
	}

	private class NodeUpdateTask extends TimerTask {

		@Override
		public void run() {
			Globals g = Globals.getInstance();
			
			if(g.getBinanceAddress() == null) {
				// no need to keep track if we do not have an address
				return;
			}
			
			try {
				BinanceDexApiRestClient client = BinanceDexApiClientFactory.newInstance().newRestClient(
	                    g.isTestnet() ? BinanceDexEnvironment.TEST_NET.getBaseUrl() : BinanceDexEnvironment.PROD.getBaseUrl());

				// check if we have a new block or not
				Infos info = client.getNodeInfo();
	            lastestBlockTime = info.getSyncInfo().getLatestBlockTime();
	            
				fees.set(client.getFees());

				account = client.getAccount(g.getBinanceAddress());
				
				for(Market m : Markets.getMarkets()) {
					if(m.getTokenID() == null)
						continue;
					logger.trace("Starting to update {} market", m.getID());
					try{
						
//						client.getOrderBook(m.getTokenID(), limit);
//						client.getTrades(request);

//						client.get24HrPriceStatistics(symbol);
//							lowestPrice24h.put(m, lowestPrice != null ? lowestPrice : trades[0].getPrice());
//							highestPrice24h.put(m, highestPrice != null ? highestPrice : trades[0].getPrice());
//							baseVolume24h.put(m, baseVolume);
//							quoteVolume24h.put(m, quoteVolume);
//							priceChangePerc24h.put(m, priceChangePerc);
					}
					catch (Exception e) {
						logger.error("Error 2: {}", e.getLocalizedMessage());
						e.printStackTrace();
					}
				}
				
				// clears any previous error
				nodeError = null;
			}
			catch (Exception rex) {
				rex.printStackTrace();
				nodeError = rex;
				logger.error("Exception {}", rex.getLocalizedMessage());
			}
		}
	}
}
