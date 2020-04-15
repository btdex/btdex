package btdex.core;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import burst.kit.entity.BurstID;
import burst.kit.entity.response.Account;
import burst.kit.entity.response.AssetBalance;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.AssetTrade;
import burst.kit.entity.response.Block;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.http.BRSError;
import burst.kit.service.BurstNodeService;

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
	private HashMap<Market, AssetBalance[]> assetBalances = new HashMap<>();
	private HashMap<Market, AssetOrder[]> askOrders = new HashMap<>();
	private HashMap<Market, AssetOrder[]> bidOrders = new HashMap<>();
	private Transaction[] txs;
	private Transaction[] utxs;
	private Block checkBlock;
	private Exception nodeError;
	private Account account;
	private FeeSuggestion suggestedFee;
	private BurstID lastBlock;
	
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
			timer.schedule(new NodeUpdateTask(), 0, 5000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public FeeSuggestion getSuggestedFee() {
		return suggestedFee;
	}

	public AssetBalance[] getAssetBalances(Market m) {
		return assetBalances.get(m);
	}
	
	public AssetOrder[] getAssetBids(Market m) {
		return bidOrders.get(m);
	}
	
	public AssetOrder[] getAssetAsks(Market m) {
		return askOrders.get(m);
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

	class NodeUpdateTask extends TimerTask {

		@Override
		public void run() {
			Globals g = Globals.getInstance();
			BurstNodeService NS = g.getNS();
			
			try {
				// we always check the unconf. transactions to get any updates
				utxs = NS.getUnconfirmedTransactions(null).blockingGet();
				
				// check if we have a new block or not
				Block[] latestBlocks = NS.getBlocks(0, 1).blockingGet();
				if(latestBlocks[0].getId().equals(lastBlock))
					return; // no need to update
				
				lastBlock = null;

				// Check if the node has the expected block
				if(checkBlock == null && g.isTestnet())
					checkBlock = NS.getBlock(BurstID.fromLong(Constants.CHECK_BLOCK_TESTNET)).blockingGet();
				try {
					account = NS.getAccount(g.getAddress()).blockingGet();
				}
				catch (Exception e) {
					if(e.getCause() instanceof BRSError) {
						BRSError error = (BRSError) e.getCause();
						if(error.getCode() != 5) { // unknown account
							nodeError = e;
							return;
						}
					}
					else {
						nodeError = e;
						return;
					}
				}
				suggestedFee = g.getNS().suggestFee().blockingGet();
				
				for(Market m : Markets.getMarkets()) {
					if(m.getTokenID() == null)
						continue;

					AssetBalance[] accounts = NS.getAssetBalances(m.getTokenID()).blockingGet();
					AssetTrade[] trades = NS.getAssetTrades(m.getTokenID(), null, 0, 200).blockingGet();
					AssetOrder[] asks = NS.getAskOrders(m.getTokenID()).blockingGet();
					AssetOrder[] bids = NS.getBidOrders(m.getTokenID()).blockingGet();

					assetBalances.put(m, accounts);
					assetTrades.put(m, trades);
					askOrders.put(m, asks);
					bidOrders.put(m, bids);
				}
				txs = NS.getAccountTransactions(g.getAddress()).blockingGet();
				
				// set we have this one updated
				lastBlock = latestBlocks[0].getId();
				// clears any previous error
				nodeError = null;
			}
			catch (RuntimeException rex) {
				rex.printStackTrace();
				nodeError = rex;
			}
		}
	}
}
