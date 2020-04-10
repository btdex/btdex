package btdex.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

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
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.TransactionAppendix;
import burst.kit.entity.response.appendix.PlaintextMessageAppendix;

public class ContractState {
	
	public enum Type {
		Invalid, Standard, NoDeposit, Buy
	}
	
	private BurstAddress address;
	private Type type;
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
	private long taker;
	private long lockMinutes;
	
	private boolean hasPending;
	
	private long rate;
	private int market;
	private int takeBlock;
	private Account marketAccount;
	
	private long lastTxId;
	
	public ContractState(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
	
	public long getMarket() {
		return market;
	}
	
	public Account getMarketAccount() {
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
	
	public boolean hasStateFlag(long flag) {
		return (state & flag) == flag;
	}

	public long getRate() {
		return rate;
	}

	public long getAmountNQT() {
		return amount;
	}
	
	public long getFeeContract() {
		return feeContract;
	}
	
	public long getActivationFee() {
		return at.getMinimumActivation().longValue();
	}
	
	public long getNewOfferFee() {
		if(type == Type.Standard) {
			return at.isFrozen() ?
					SellContract.REUSE_OFFER_FEE :
						SellContract.NEW_OFFER_FEE;
		}
		if(type == Type.Buy)
			return at.isFrozen() ?
					BuyContract.REUSE_OFFER_FEE :
						BuyContract.NEW_OFFER_FEE;

		return getActivationFee();
	}
	
	public boolean hasPending() {
		return hasPending;
	}
	
	public String getAmount() {
		double dvalue = (double)amount / Contract.ONE_BURST;
		return NumberFormatting.BURST.format(dvalue);
	}

	public long getSecurityNQT() {
		return security;
	}
	
	public long getTaker() {
		return taker;
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
	public static BurstID addContracts(HashMap<BurstAddress, ContractState> map, BurstID mostRecent){
		Globals g = Globals.getInstance();
		BT.setNodeInstance(g.getNS());
		
		BurstAddress[] atIDs = g.getNS().getAtIds().blockingGet();
		
		BurstID first = null;
		BurstID idLimit = BurstID.fromLong(g.isTestnet() ?
				"9601465021860021685" : "17916279999448178140");
		
		// check for the pending transactions
		Transaction[] utxs = g.getNS().getUnconfirmedTransactions(g.getAddress()).blockingGet();
		
		// reverse order to get the more recent ones first
		for (int i = atIDs.length-1; i >= 0; i--) {
			BurstAddress ad = atIDs[i];
			
			if(first == null)
				first = ad.getBurstID();
			
			// avoid running all IDs, we know these past one are useless
			if(ad.getBurstID().getSignedLongId() == idLimit.getSignedLongId())
				break;
			// already visited, no need to continue
			if(mostRecent!=null && ad.getBurstID().getSignedLongId() == mostRecent.getSignedLongId())
				break;
			
			// If the map already have this one stop, since they come in order
			if(map.containsKey(ad))
				break;
			
			AT at = g.getNS().getAt(ad).blockingGet();
			Type type = Type.Invalid;
			
			// check the code (should match perfectly)
			if(Contracts.checkContractCode(at, Contracts.getContractCode()))
				type = Type.Standard;
			else if(Contracts.checkContractCode(at, Contracts.getContractBuyCode()))
				type = Type.Buy;
			else if(Contracts.checkContractCode(at, Contracts.getContractNoDepositCode()))
				type = Type.NoDeposit;
			
			if(type!=Type.Invalid) {
				ContractState s = new ContractState(type);
				s.at = at;
				
				// Check some immutable variables
				s.compiler = Contracts.getContract(type);
				s.mediator1 = s.getContractFieldValue("mediator1");
				s.mediator2 = s.getContractFieldValue("mediator2");
				s.feeContract = s.getContractFieldValue("feeContract");
				
				// Check if the immutable variables are valid
				if(g.getMediators().areMediatorsAccepted(s)
						&& Constants.FEE_CONTRACT == s.getFeeContract()){
					s.hasPending = s.updateState(at) || s.processTransactions(utxs, s.takeBlock);
					
					map.put(ad, s);
				}
			}
		}
				
		return first;
	}	
	
	public void update() {
		updateState(null);
	}
	
    private long getContractFieldValue(String field) {
    	int address = compiler.getFieldAddress(field);
        byte[] data = at.getMachineData();
        ByteBuffer b = ByteBuffer.wrap(data);
        b.order(ByteOrder.LITTLE_ENDIAN);

        return b.getLong(address * 8);
    }
    
	private boolean updateState(AT at) {
		Globals g = Globals.getInstance();
		
		if(at == null)
			at = g.getNS().getAt(address).blockingGet();
		
		this.at = at;
		this.address = at.getId();
		this.balance = at.getBalance();
		
		// update variables that can change over time
		if(type == Type.Standard || type == Type.Buy) {
			this.state = getContractFieldValue("state");
			this.amount = getContractFieldValue("amount");
			this.security = getContractFieldValue("security");
			this.taker = getContractFieldValue("taker");
		}
		else if(type == Type.NoDeposit) {
			this.state = getContractFieldValue("state");
			this.lockMinutes = getContractFieldValue("lockMinutes");
		}
		
		Transaction[] txs = null;
		boolean hasPending = false;

		if(state != SellContract.STATE_FINISHED) {
			// check rate, type, etc. from transaction history
			txs = g.getNS().getAccountTransactions(this.address).blockingGet();
			takeBlock = findTakeBlock(txs);
			hasPending = processTransactions(txs, takeBlock);
		}
		return hasPending;
	}
	
	private int findTakeBlock(Transaction[] txs) {
		int takeBlock = 0;
		if(this.state > SellContract.STATE_TAKEN) {
			// We need to find out what is the block of the take transaction (price definition should be after that)
			for(Transaction tx : txs) {
				if(tx.getSender().getSignedLongId() == this.taker &&
						tx.getAppendages()!=null && tx.getAppendages().length==1 &&
						tx.getAppendages()[0] instanceof PlaintextMessageAppendix) {
					PlaintextMessageAppendix msg = (PlaintextMessageAppendix) tx.getAppendages()[0];
					if(!msg.isText() && msg.getMessage().startsWith(Contracts.getContractUpdateHash())) {
						// should be a hexadecimal message
						takeBlock = tx.getBlockHeight() - Constants.PRICE_NCONF;
						break;
					}
				}
			}
		}
		return takeBlock;
	}
	
	private boolean processTransactions(Transaction[] txs, int blockHeightLimit) {
		Globals g = Globals.getInstance();
		boolean hasPending = false;

		for(Transaction tx : txs) {
			if(tx.getId().getSignedLongId() == lastTxId)
				break;
			
			// Only transactions for this contract
			if(!tx.getRecipient().equals(getAddress()))
				continue;
			
			// We only accept configurations with 2 confirmations or more
			// but also get pending info from the user
			if(tx.getConfirmations() < Constants.PRICE_NCONF
					|| (blockHeightLimit > 0 && tx.getBlockHeight() > blockHeightLimit) ) {
				if(tx.getSender().equals(g.getAddress())) {
					hasPending = true;
				}
				else
					continue;
			}
			
			// order configurations should be set by the creator
			if(!tx.getSender().equals(at.getCreator()))
				continue;
			
			if(tx.getRecipient().getSignedLongId() == address.getSignedLongId()
					&& tx.getSender().equals(at.getCreator())
					&& tx.getType() == 1 /* TYPE_MESSAGING */
					&& tx.getSubtype() == 0 /* SUBTYPE_MESSAGING_ARBITRARY_MESSAGE */
					&& tx.getAppendages()!=null && tx.getAppendages().length > 0) {
				
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
						if(type != Type.Buy) {
							for(Market m : Markets.getMarkets()) {
								if(m.getID() == market) {
									marketAccount = m.parseAccount(accountFields);
									break;
								}
							}
						}
						
						// set this as the accepted last TxId
						if(tx.getConfirmations() >= Constants.PRICE_NCONF) {
							lastTxId = tx.getId().getSignedLongId();
						
							// done, only the more recent (2 confirmations) matters
							break;
						}
					}
					catch (Exception e) {
						// we ignore invalid messages
					}
				}
			}
		}
		return hasPending;
	}
}
