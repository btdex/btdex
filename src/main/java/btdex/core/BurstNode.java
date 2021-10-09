package btdex.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import btdex.markets.MarketTRT;
import signumj.entity.SignumAddress;
import signumj.entity.SignumID;
import signumj.entity.SignumValue;
import signumj.entity.response.Account;
import signumj.entity.response.AssetBalance;
import signumj.entity.response.AssetOrder;
import signumj.entity.response.AssetTrade;
import signumj.entity.response.Block;
import signumj.entity.response.Constants.TransactionType;
import signumj.entity.response.Constants.TransactionType.Subtype;
import signumj.entity.response.FeeSuggestion;
import signumj.entity.response.MiningInfo;
import signumj.entity.response.Transaction;
import signumj.service.NodeService;

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
	private HashMap<Market, List<AssetBalance>> assetBalanceAllAccounts = new HashMap<>();
	private HashMap<Market, AssetOrder[]> askOrders = new HashMap<>();
	private HashMap<Market, AssetOrder[]> bidOrders = new HashMap<>();
	
	private HashMap<Market, SignumValue> baseVolume24h = new HashMap<>();
	private HashMap<Market, SignumValue> quoteVolume24h = new HashMap<>();
	private HashMap<Market, Double> priceChangePerc24h = new HashMap<>();
	private HashMap<Market, SignumValue> highestPrice24h = new HashMap<>();
	private HashMap<Market, SignumValue> lowestPrice24h = new HashMap<>();	
	
	private Transaction[] txs;
	private Transaction[] utxs;
	private Block checkBlock;
	private Exception nodeError;
	private AtomicReference<MiningInfo> miningInfo = new AtomicReference<>();
	private AtomicReference<Account> account = new AtomicReference<>();
	private AtomicReference<SignumAddress> rewardRecipient = new AtomicReference<>();
	private FeeSuggestion suggestedFee;
	private signumj.entity.response.Constants constants;
	private SignumID lastBlock;
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
	
	public Subtype getTransactionSubtype(int type, int subtype) {
		TransactionType[] txtypes = constants != null ? constants.getTransactionTypes() : null;
		if(txtypes != null) {
			for(TransactionType t : txtypes) {
				if(t.getType() == type) {
					for(Subtype s : t.getSubtypes()) {
						if(s.getSubtype() == subtype) {
							return s;
						}
					}
				}
			}
		}
		return null;
	}

	public AssetBalance getAssetBalances(Market m) {
		return assetBalances.get(m);
	}

	public List<AssetBalance> getAssetBalanceAllAccounts(Market m) {
		return assetBalanceAllAccounts.get(m);
	}

	public AssetOrder[] getAssetBids(Market m) {
		return bidOrders.get(m);
	}

	public AssetOrder[] getAssetAsks(Market m) {
		return askOrders.get(m);
	}
	
	public SignumValue getBaseVolume24h(Market m) {
		return baseVolume24h.get(m);
	}

	public SignumValue getQuoteVolume24h(Market m) {
		return quoteVolume24h.get(m);
	}

	public double getPriceChangePerc24h(Market m) {
		return priceChangePerc24h.get(m);
	}
	
	public SignumValue getHighestPrice24h(Market m) {
		return highestPrice24h.get(m);
	}
	
	public SignumValue getLowestPrice24h(Market m) {
		return lowestPrice24h.get(m);
	}

	public Transaction[] getAccountTransactions() {
		return txs;
	}

	public Transaction[] getUnconfirmedTransactions() {
		return utxs;
	}

	public Account getAccount() {
		return account.get();
	}

	public MiningInfo getMiningInfo() {
		return miningInfo.get();
	}

	public SignumAddress getRewardRecipient() {
		return rewardRecipient.get();
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
			NodeService NS = g.getNS();

			try {
				// we always check the unconf. transactions to get any updates
				utxs = NS.getUnconfirmedTransactions(null).blockingGet();

				// check if we have a new block or not
				Block[] latestBlocks = NS.getBlocks(0, 1).blockingGet();
				latestBlock = latestBlocks[0];
				if(latestBlocks[0].getId().equals(lastBlock))
					return; // no need to update

				lastBlock = null;

				suggestedFee = NS.suggestFee().blockingGet();
				constants = NS.getConstants().blockingGet();
				
				Mediators mediators = g.getMediators();

				for(Market m : Markets.getMarkets()) {
					if(m.getTokenID() == null)
						continue;
					logger.trace("Starting to update {} market", m.getID());
					try{
						List<AssetBalance> allBalances = new ArrayList<>();
						AssetBalance balance = null;

						Integer first = 0;
						Integer delta = 400;
						while(true) {
							AssetBalance[] accounts = NS.getAssetBalances(m.getTokenID(), first, first+delta).blockingGet();
							if(accounts == null || accounts.length == 0)
								break;
							for(AssetBalance a : accounts) {
								allBalances.add(a);
							}

							for(AssetBalance b : accounts) {
								if(b.getAccountAddress().equals(g.getAddress())) {
									balance = b;
								}

								if(m.getTokenID().equals(TRT.getTokenID())) {
									for (int i = 0; i < mediators.getMediators().length; i++) {
										SignumID mediator = mediators.getMediators()[i];
										if(b.getAccountAddress().getSignumID().equals(mediator)) {
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
						
						assetBalances.put(m, balance);
						assetBalanceAllAccounts.put(m, allBalances);
						assetTrades.put(m, trades);
						askOrders.put(m, asks);
						bidOrders.put(m, bids);
						
						// If we don't have any trades, there is no trade history
						if(trades!=null && trades.length>0) {
							SignumValue lowestPrice = null;
							SignumValue highestPrice = null;
							Date past24h = new Date(System.currentTimeMillis() - 24 * 3600_000);
							SignumValue baseVolume = SignumValue.fromNQT(0L);
							SignumValue quoteVolume = SignumValue.fromNQT(0L);
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
						}
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
					miningInfo.set(NS.getMiningInfo().blockingFirst());
					rewardRecipient.set(NS.getRewardRecipient(g.getAddress()).blockingGet());
					Boolean getCommitmentStats = null;
					if(miningInfo.get().getAverageCommitmentNQT() > 0) {
						// Only request commitment stats if there is support on the node
						getCommitmentStats = Boolean.TRUE;
					}
					
					account.set(NS.getAccount(g.getAddress(), null, getCommitmentStats, getCommitmentStats).blockingGet());
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
