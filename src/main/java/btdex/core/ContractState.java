package btdex.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import org.bouncycastle.util.encoders.Hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import bt.compiler.Method;
import btdex.sc.BuyContract;
import btdex.sc.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstTimestamp;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.TransactionAppendix;
import burst.kit.entity.response.appendix.PlaintextMessageAppendix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ContractState {

	private BurstAddress address;
	private ContractType type;
	private AT at;
	private BurstValue balance;

	private long mediator1;
	private long mediator2;
	private long offerType;
	private long feeContract;
	private Compiler compiler;

	private long state;
	private long amount;
	private long security;
	private long fee;
	private long taker;

	private long lockMinutes;

	private boolean hasPending;

	private long rate;
	private int market;
	private int takeBlock;
	private BurstTimestamp takeTimestamp;
	private String marketAccount;

	private long lastTxId;
	private ArrayList<ContractTrade> trades = new ArrayList<>();
	private long rateHistory;
	private long marketHistory;
	private int blockHistory;

	private static Logger logger = LogManager.getLogger();

	public ContractState(ContractType type, AT at) {
		this.type = type;
		this.at = at;
		
		// Check some immutable variables
		compiler = Contracts.getCompiler(type);
		mediator1 = getContractFieldValue("mediator1");
		mediator2 = getContractFieldValue("mediator2");
		feeContract = getContractFieldValue("feeContract");
	}

	public boolean hasStateFlag(long flag) {
		return (state & flag) == flag;
	}

	public BurstTimestamp getTakeTimestamp() {
		return takeTimestamp;
	}
	
	@Override
	public String toString() {
		return address.toString() + ", " + market + ", " + rate;
	}

	public long getNewOfferFee() {
		if(type == ContractType.SELL) {
			return at.isFrozen() ?
					SellContract.REUSE_OFFER_FEE :
						SellContract.NEW_OFFER_FEE;
		}
		if(type == ContractType.BUY)
			return at.isFrozen() ?
					BuyContract.REUSE_OFFER_FEE :
						BuyContract.NEW_OFFER_FEE;

		return getActivationFee();
	}

	public String getAmount() {
		double dvalue = (double)amount / Contract.ONE_BURST;
		return NumberFormatting.BURST.format(dvalue);
	}

	public String getSecurity() {
		double dvalue = (double)security / Contract.ONE_BURST;
		return NumberFormatting.BURST.format(dvalue);
	}

	public Method getMethod(String method) {
		return compiler.getMethod(method);
	}

	/**
	 * Add all contracts with matching machine code to the given map.
	 *
	 * First call should be with an empty map and then the map can be reused to
	 * add possibly recently registered contracts.
	 *
	 * @param map
	 *
	 * @return the most recent ID visited
	 */
	public static BurstID addContracts(HashMap<BurstAddress, ContractState> map, BurstID mostRecent) {
		Globals g = Globals.getInstance();
		BT.setNodeInstance(g.getNS());

		BurstAddress[] atIDs = g.getNS().getAtIds().blockingGet();

		BurstID first = null;
		BurstID idLimit = BurstID.fromLong(g.isTestnet() ?
				"12494358857357719882" : "17760275010219707380");
		
		// reverse order to get the more recent ones first
		for (int i = atIDs.length - 1; i >= 0; i--) {
			BurstAddress ad = atIDs[i];

			if (first == null)
				first = ad.getBurstID();

			// avoid running all IDs, we know these past one are useless
			if (ad.getBurstID().getSignedLongId() == idLimit.getSignedLongId())
				break;
			// already visited, no need to continue
			if (mostRecent != null && ad.getBurstID().getSignedLongId() == mostRecent.getSignedLongId())
				break;

			// If the map already have this one stop, since they come in order
			if (map.containsKey(ad))
				break;

			AT at = g.getNS().getAt(ad).blockingGet();
			ContractType type = ContractType.INVALID;

			// check the code (should match perfectly)
			if (Contracts.checkContractCode(at, Contracts.getCodeSell()))
				type = ContractType.SELL;
			else if (Contracts.checkContractCode(at, Contracts.getCodeBuy()))
				type = ContractType.BUY;
			else if (Contracts.checkContractCode(at, Contracts.getCodeNoDeposit()))
				type = ContractType.NO_DEPOSIT;

			if (type != ContractType.INVALID) {
				ContractState s = new ContractState(type, at);

				logger.debug("Contract {} added for {}", at.getId(), s.type);

				// Check if the immutable variables are valid
				if (g.getFeeContract() == s.getFeeContract()) {
					s.updateState(at, null, false);
					map.put(ad, s);
				}
			}
		}

		return first;
	}

	public void update(Transaction[] utxs, boolean onlyUnconf) {
		updateState(onlyUnconf ? at : null, utxs, onlyUnconf);
	}

	private long getContractFieldValue(String field) {
		int address = compiler.getFieldAddress(field);
		byte[] data = at.getMachineData();
		ByteBuffer b = ByteBuffer.wrap(data);
		b.order(ByteOrder.LITTLE_ENDIAN);

		return b.getLong(address * 8);
	}

	private void updateState(AT at, Transaction[] utxs, boolean onlyUnconf) {
		Globals g = Globals.getInstance();

		if(at == null)
			at = g.getNS().getAt(address).blockingGet();

		this.at = at;
		this.address = at.getId();
		this.balance = at.getBalance();

		if(at.isDead())
			type = ContractType.INVALID;

		// update variables that can change over time
		if(type == ContractType.SELL || type == ContractType.BUY) {
			this.state = getContractFieldValue("state");
			this.amount = getContractFieldValue("amount");
			this.security = getContractFieldValue("security");
			this.fee = getContractFieldValue("fee");
			this.taker = getContractFieldValue("taker");
		}
		else if(type == ContractType.NO_DEPOSIT) {
			this.state = getContractFieldValue("state");
			this.lockMinutes = getContractFieldValue("lockMinutes");
		}

		// check rate, type, etc. from transaction history
		boolean hasPending = false;
		if(!onlyUnconf) {
			Transaction[] txs = g.getNS().getAccountTransactions(this.address, null, null, false).blockingGet();
			findCurrentTakeBlock(txs);
			buildHistory(txs);
			hasPending = processTransactions(txs);
		}
		hasPending = hasPending || processTransactions(utxs);
		if(!onlyUnconf || hasPending)
			this.hasPending = hasPending;
	}

	private void findCurrentTakeBlock(Transaction[] txs) {
		int takeBlock = 0;
		BurstTimestamp takeTimestamp = null;
		if(this.state > SellContract.STATE_TAKEN) {
			// We need to find out what is the block of the take transaction (price definition should be after that)
			for(Transaction tx : txs) {
				if(takeBlock > 0 && tx.getBlockHeight() < takeBlock && marketAccount != null)
					break;

				if(tx.getSender().getSignedLongId() == this.taker &&
						tx.getAppendages()!=null && tx.getAppendages().length==1 &&
						tx.getAppendages()[0] instanceof PlaintextMessageAppendix) {
					PlaintextMessageAppendix msg = (PlaintextMessageAppendix) tx.getAppendages()[0];
					if(!msg.isText() && msg.getMessage().startsWith(Contracts.getContractTakeHash(type))) {
						takeBlock = tx.getBlockHeight();
						takeTimestamp = tx.getTimestamp();
					}
				}

				// we also look for the address definition of a taken buy order
				// (should be more recent than the takeBlock, this is taken care with the 'break' on the loop start)
				if(type == ContractType.BUY && tx.getSender().getSignedLongId() == this.taker
						&& tx.getType() == 1 /* TYPE_MESSAGING */
						&& tx.getSubtype() == 0 /* SUBTYPE_MESSAGING_ARBITRARY_MESSAGE */) {
					TransactionAppendix append = tx.getAppendages()[0];
					if(append instanceof PlaintextMessageAppendix) {
						PlaintextMessageAppendix appendMessage = (PlaintextMessageAppendix) append;
						try {
							String jsonData = appendMessage.getMessage();
							JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
							JsonElement accountJson = json.get("account");
							String accountFields = null;
							if(accountJson!=null)
								accountFields = accountJson.getAsString();

							// parse the account fields (buy do not have it)
							if(accountFields != null) {
								for(Market m : Markets.getMarkets()) {
									if(m.getID() == market) {
										marketAccount = accountFields;
										break;
									}
								}
							}
						}
						catch (Exception e) {
							// we ignore invalid messages
							logger.trace(e.getLocalizedMessage());
						}
					}
				}
			}
		}
		this.takeBlock = takeBlock;
		this.takeTimestamp = takeTimestamp;
	}

	private boolean processTransactions(Transaction[] txs) {
		if(txs == null)
			return false;

		Globals g = Globals.getInstance();
		boolean hasPending = false;
		
		for(Transaction tx : txs) {
			if(tx.getId().getSignedLongId() == lastTxId && takeBlock == 0)
				break;

			// Only transactions for this contract
			if(tx.getRecipient()==null || !tx.getRecipient().equals(getAddress()))
				continue;

			// We only accept configurations with 2 confirmations or more
			// but also get pending info from the user
			if(tx.getConfirmations() < Constants.PRICE_NCONF) {
				if(tx.getSender().equals(g.getAddress()) ||
						(tx.getSender().getSignedLongId() == getTaker() && tx.getSender().equals(g.getAddress()) )) {
					logger.debug("Pending tx for {}", getAddress());
					hasPending = true;
				}
				else
					continue;
			}

			// order configurations should be set by the creator (or the taker of a buy is sending his address)
			if(!tx.getSender().equals(at.getCreator()))
				continue;

			if(tx.getType() == 1 /* TYPE_MESSAGING */
					&& tx.getSubtype() == 0 /* SUBTYPE_MESSAGING_ARBITRARY_MESSAGE */
					&& tx.getAppendages()!=null && tx.getAppendages().length > 0) {
				if(takeBlock > 0 && tx.getBlockHeight() > takeBlock)
					continue; // we ignore any configuration after the take block

				TransactionAppendix append = tx.getAppendages()[0];
				if(append instanceof PlaintextMessageAppendix) {
					PlaintextMessageAppendix appendMessage = (PlaintextMessageAppendix) append;

					try {
						String jsonData = appendMessage.getMessage();
						JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
						JsonElement marketJson = json.get("market");
						JsonElement rateJson = json.get("rate");
						JsonElement accountJson = json.get("account");
						market = -1;
						String accountFields = null;
						rate = -1;
						if(marketJson!=null)
							market = Integer.parseInt(marketJson.getAsString());
						if(accountJson!=null)
							accountFields = accountJson.getAsString();
						if(rateJson!=null)
							rate = Long.parseLong(rateJson.getAsString());

						// parse the account fields (buy do not have it)
						if(accountFields != null) {
							for(Market m : Markets.getMarkets()) {
								if(m.getID() == market) {
									// check is valid
									m.parseAccount(accountFields);
									marketAccount = accountFields;
									break;
								}
							}
						}

						// set this as the accepted last TxId
						if(tx.getConfirmations() >= Constants.PRICE_NCONF && takeBlock == 0) {
							lastTxId = tx.getId().getSignedLongId();
							logger.debug("last tx={} for {}", tx.getId(), getAddress());

							// done, only the more recent (2 confirmations) matters
							break;
						}
						// we should stop on the first valid configuration before the take block
						if(takeBlock > 0 && tx.getBlockHeight() < takeBlock)
							break;
					}
					catch (Exception e) {
						// we ignore invalid messages
						logger.trace(e.getLocalizedMessage());
					}

				}
			}
		}
		return hasPending;
	}

	private void buildHistory(Transaction[] txs) {
		if(txs == null || txs.length == 0
				|| txs[0].getBlockHeight() <= blockHistory) // we already have the more recent one
			return;

		rateHistory = 0;
		marketHistory = 0;

		for (int i = txs.length -1 ; i >= 0; i--) {
			// in reverse order so we have the price available when we see the 'take'
			Transaction tx = txs[i];

			// if we already have this one
			if(tx.getBlockHeight() <= blockHistory)
				continue;

			if(tx.getRecipient().equals(address)
					&& tx.getAppendages()!=null && tx.getAppendages().length > 0
					&& tx.getAppendages()[0] instanceof PlaintextMessageAppendix) {

				PlaintextMessageAppendix appendMessage = (PlaintextMessageAppendix) tx.getAppendages()[0];
				String messageString = appendMessage.getMessage();
				if(tx.getSender().equals(at.getCreator()) && messageString.startsWith("{")) {
					// price update
					try {
						JsonObject json = JsonParser.parseString(messageString).getAsJsonObject();
						JsonElement marketJson = json.get("market");
						JsonElement rateJson = json.get("rate");
						if(marketJson!=null)
							marketHistory = Long.parseLong(marketJson.getAsString());
						if(rateJson!=null)
							rateHistory = Long.parseLong(rateJson.getAsString());
					}
					catch (Exception e) {
						logger.debug(e.getLocalizedMessage());
						// we ignore invalid messages
					}
				}
				else if(rateHistory > 0L && marketHistory > 0L // we only want to register trades we know the price
						&& !appendMessage.isText() && messageString.length() == 64
						&& messageString.startsWith(Contracts.getContractTakeHash(type))) {
					// the take message (we are not so strict here as this is not vital information)
					// so we can have false positives here, like multiple takes on the same order or invalid takes
					// being account
					byte []messageBytes = Hex.decode(messageString);
					ByteBuffer b = ByteBuffer.wrap(messageBytes);
					b.order(ByteOrder.LITTLE_ENDIAN);
					b.getLong(); // method hash
					long security = b.getLong();
					long amount = b.getLong();

					ContractTrade trade = new ContractTrade(this, tx, rateHistory, security, amount, marketHistory);
					trades.add(trade);
				}
			}
		}

		blockHistory = txs[0].getBlockHeight();
	}


	public boolean hasPending() {
		return hasPending;
	}

	public long getSecurityNQT() {
		return security;
	}

	public long getTaker() {
		return taker;
	}

	public long getRate() {
		return rate;
	}

	public long getAmountNQT() {
		return amount;
	}

	public long getFeeNQT() {
		return fee;
	}

	public long getFeeContract() {
		return feeContract;
	}

	public long getActivationFee() {
		return at.getMinimumActivation().longValue();
	}

	public ContractType getType() {
		return type;
	}

	public long getMarket() {
		return market;
	}

	public String getMarketAccount() {
		return marketAccount;
	}

	public BurstAddress getAddress() {
		return address;
	}

	public BurstAddress getCreator() {
		return at.getCreator();
	}

	public BurstValue getBalance() {
		return balance;
	}

	public long getMediator1() {
		return mediator1;
	}

	public long getMediator2() {
		return mediator2;
	}

	public long getOfferType() {
		return offerType;
	}

	public long getState() {
		return state;
	}

	public long getLockMinutes() {
		return lockMinutes;
	}

	public int getATVersion() {
		return at.getVersion();
	}
	
	public long getDisputeAmount(boolean fromCreator, boolean toCreator) {
		if(fromCreator)
			return getContractFieldValue(toCreator ? "disputeCreatorAmountToCreator" : "disputeCreatorAmountToTaker");
		return getContractFieldValue(toCreator ? "disputeTakerAmountToCreator" : "disputeTakerAmountToTaker");
	}

	public ArrayList<ContractTrade> getTrades() {
		return trades;
	}
}
