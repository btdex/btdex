package btdex.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import btdex.markets.MarketTRT;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.Account;
import burst.kit.entity.response.AssetBalance;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.AssetTrade;
import burst.kit.entity.response.Block;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.Transaction;
import burst.kit.service.BurstNodeService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Keeps updated the relevant information from a Burst node.
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
public class BurstNode {

	private HashMap<Market, AssetTrade[]> assetTrades = new HashMap<>();
	private HashMap<Market, AssetBalance> assetBalances = new HashMap<>();
	private HashMap<Market, AssetOrder[]> askOrders = new HashMap<>();
	private HashMap<Market, AssetOrder[]> bidOrders = new HashMap<>();
	
	private HashMap<Market, BurstValue> baseVolume24h = new HashMap<>();
	private HashMap<Market, BurstValue> quoteVolume24h = new HashMap<>();
	private HashMap<Market, Double> priceChangePerc24h = new HashMap<>();
	private HashMap<Market, BurstValue> highestPrice24h = new HashMap<>();
	private HashMap<Market, BurstValue> lowestPrice24h = new HashMap<>();	
	
	private Transaction[] txs;
	private Transaction[] utxs;
	private Block checkBlock;
	private Exception nodeError;
	private Account account;
	private FeeSuggestion suggestedFee;
	private BurstID lastBlock;
	private Block latestBlock;

	private static Logger logger = LogManager.getLogger();

	private static Market TRT = new MarketTRT();

	static BurstNode INSTANCE;

	public static BurstNode getInstance() {
		if(INSTANCE==null)
			INSTANCE = new BurstNode();
		return INSTANCE;
	}

	public BurstNode() {
		try {
			// start the node updater thread
			Timer timer = new Timer("node update");
			timer.schedule(new NodeUpdateTask(), 0, 3000);
			logger.info("BurstNode update thread started");
		}
		catch (Exception e) {
			logger.error("Error 1: {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public FeeSuggestion getSuggestedFee() {
		return suggestedFee;
	}

	public AssetBalance getAssetBalances(Market m) {
		return assetBalances.get(m);
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

	public Transaction[] getAccountTransactions() {
		return txs;
	}

	public Transaction[] getUnconfirmedTransactions() {
		return utxs;
	}

	public Account getAccount() {
		return account;
	}

	public Exception getNodeException() {
		return nodeError;
	}

	public Block getLatestBlock() {
		return latestBlock;
	}

	public Block getCheckBlock() {
		return checkBlock;
	}

	public AssetTrade[] getAssetTrades(Market id){
		return assetTrades.get(id);
	}

	/**
	 * Force an update even without a new block (e.g. when a new market is introduced)
	 */
	public void update() {
		lastBlock = null;
	}

	private class NodeUpdateTask extends TimerTask {

		@Override
		public void run() {
			Globals g = Globals.getInstance();
			BurstNodeService NS = g.getNS();

			try {
				// we always check the unconf. transactions to get any updates
				utxs = NS.getUnconfirmedTransactions(null).blockingGet();

				// check if we have a new block or not
				Block[] latestBlocks = NS.getBlocks(0, 1).blockingGet();
				latestBlock = latestBlocks[0];
				if(latestBlocks[0].getId().equals(lastBlock))
					return; // no need to update

				lastBlock = null;

				suggestedFee = g.getNS().suggestFee().blockingGet();

				Mediators mediators = g.getMediators();

				for(Market m : Markets.getMarkets()) {
					if(m.getTokenID() == null)
						continue;
					logger.trace("Starting to update {} market", m.getID());
					try{
						AssetBalance balance = null;

						Integer first = 0;
						Integer delta = 400;
						while(true) {
							AssetBalance[] accounts = NS.getAssetBalances(m.getTokenID(), first, first+delta).blockingGet();
							if(accounts == null || accounts.length == 0)
								break;

							for(AssetBalance b : accounts) {
								if(b.getAccountAddress().equals(g.getAddress())) {
									balance = b;
								}

								if(m.getTokenID().equals(TRT.getTokenID())) {
									for (int i = 0; i < mediators.getMediators().length; i++) {
										BurstID mediator = mediators.getMediators()[i];
										if(b.getAccountAddress().getBurstID().equals(mediator)) {
											mediators.setMediatorBalance(i, b.getBalance());
										}
									}
								}
							}
							first += delta;
						}

						AssetTrade[] trades = NS.getAssetTrades(m.getTokenID(), null, 0, 200).blockingGet();
						AssetOrder[] asks = NS.getAskOrders(m.getTokenID()).blockingGet();
						AssetOrder[] bids = NS.getBidOrders(m.getTokenID()).blockingGet();
						
						BurstValue lowestPrice = null;
						BurstValue highestPrice = null;
						Date past24h = new Date(System.currentTimeMillis() - 24 * 3600_000);
						BurstValue baseVolume = BurstValue.fromPlanck(0L);
						BurstValue quoteVolume = BurstValue.fromPlanck(0L);
						double priceChangePerc = 0.0;
						for(AssetTrade t : trades) {
							if(t.getTimestamp().getAsDate().before(past24h)) {
								priceChangePerc = 100d*(trades[0].getPrice().doubleValue() - t.getPrice().doubleValue())/trades[0].getPrice().doubleValue();
								break;
							}
							if(lowestPrice == null || t.getPrice().compareTo(lowestPrice) < 0)
								lowestPrice = t.getPrice();
							if(highestPrice == null || t.getPrice().compareTo(highestPrice) > 0)
								highestPrice = t.getPrice();
							
							baseVolume = baseVolume.add(t.getQuantity());
							quoteVolume = quoteVolume.add(t.getQuantity().multiply(t.getPrice().longValue()));
						}
						lowestPrice24h.put(m, lowestPrice != null ? lowestPrice : trades[0].getPrice());
						highestPrice24h.put(m, highestPrice != null ? highestPrice : trades[0].getPrice());
						baseVolume24h.put(m, baseVolume);
						quoteVolume24h.put(m, quoteVolume);
						priceChangePerc24h.put(m, priceChangePerc);

						assetBalances.put(m, balance);
						assetTrades.put(m, trades);
						askOrders.put(m, asks);
						bidOrders.put(m, bids);
					}
					catch (Exception e) {
						logger.error("Error 2: {}", e.getLocalizedMessage());
						e.printStackTrace();
					}
				}

				// Check if the node has the expected block
				if(checkBlock == null) {
					checkBlock = NS.getBlock(Constants.CHECK_HEIGHT).blockingGet();
				}
				try {
					account = NS.getAccount(g.getAddress()).blockingGet();
				}
				catch (Exception e) {
					nodeError = e;
					logger.debug("Error 3: {}", e.getLocalizedMessage());
					return;
				}
				txs = NS.getAccountTransactions(g.getAddress(), 0, 100, true).blockingGet();

				// set we have this one updated
				lastBlock = latestBlocks[0].getId();
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
