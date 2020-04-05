package btdex.core;

import java.util.HashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.TransactionAppendix;
import burst.kit.entity.response.appendix.PlaintextMessageAppendix;

public class ContractState {
	
	enum Type {
		Invalid, Standard, NoDeposit
	}
	
	private BurstAddress address;
	private Type type;
	private AT at;
	private BurstValue balance;
	
	private long mediator1;
	private long mediator2;
	private long offerType;
	private long feeContract;
	
	private long state;
	private long amount;
	private long security;
	private long lockMinutes;
	
	private long rate;
	private int market;
	private String account;
	
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
	
	public String getAmount() {
		double dvalue = (double)amount / Contract.ONE_BURST;
		return NumberFormatting.BURST.format(dvalue);
	}

	public long getSecurityNQT() {
		return security;
	}
	
	public String getSecurity() {
		double dvalue = (double)security / Contract.ONE_BURST;
		return NumberFormatting.BURST.format(dvalue);
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
			if(Contracts.checkContractCode(at))
				type = Type.Standard;
			else if(Contracts.checkContractCodeNoDeposit(at))
				type = Type.NoDeposit;
			
			if(type!=Type.Invalid) {
				ContractState s = new ContractState(type);
				
				// Check some immutable variables
				if(type == Type.Standard) {
					Compiler contract = Contracts.getContract();
					s.mediator1 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator1"));
					s.mediator2 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator2"));
					s.feeContract = BT.getContractFieldValue(at, contract.getFieldAddress("feeContract"));
				}
				else if(type == Type.NoDeposit) {
					Compiler contract = Contracts.getContractNoDeposit();
					s.mediator1 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator1"));
					s.mediator2 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator2"));
					s.feeContract = BT.getContractFieldValue(at, contract.getFieldAddress("feeContract"));
				}
				
				// Check if the immutable variables are valid
				if(g.getMediators().isMediatorAccepted(s.getMediator1())
						&& g.getMediators().isMediatorAccepted(s.getMediator2())
						&& Constants.FEE_CONTRACT == s.getFeeContract()){
					s.updateState(at);
					map.put(ad, s);
				}
			}
		}
		
		return first;
	}	
	
	public void update() {
		updateState(at);
	}
	
	void updateState(AT at) {
		Globals g = Globals.getInstance();
		
		if(at == null)
			at = g.getNS().getAt(address).blockingGet();
		
		this.at = at;
		this.address = at.getId();
		this.balance = at.getBalance();
		
		// update variables that can change over time
		if(type == Type.Standard) {
			Compiler contract = Contracts.getContract();
			this.state = BT.getContractFieldValue(at, contract.getFieldAddress("state"));
			this.amount = BT.getContractFieldValue(at, contract.getFieldAddress("amount"));
			this.security = BT.getContractFieldValue(at, contract.getFieldAddress("security"));
		}
		else if(type == Type.NoDeposit) {
			Compiler contract = Contracts.getContractNoDeposit();
			this.state = BT.getContractFieldValue(at, contract.getFieldAddress("state"));
			this.lockMinutes = BT.getContractFieldValue(at, contract.getFieldAddress("lockMinutes"));
		}
		
		// check rate, type, etc. from transaction
		Transaction[] txs = g.getNS().getAccountTransactions(this.address).blockingGet();
		for(Transaction tx : txs) {
			if(tx.getId().getSignedLongId() == lastTxId)
				break;
			
			// we only accept configurations with 2 confirmations or more
			if(tx.getConfirmations() < Constants.PRICE_NCONF)
				continue;
			
			// order configurations should be set by the creator
			if(tx.getSender().getSignedLongId() != at.getCreator().getSignedLongId())
				continue;
			
			if(tx.getRecipient().getSignedLongId() == address.getSignedLongId()
					&& tx.getSender().getBurstID().getSignedLongId() == at.getCreator().getSignedLongId()
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
						account = null;
						rate = -1;
						if(marketJson!=null)
							market = Integer.parseInt(marketJson.getAsString());
						if(accountJson!=null)
							account = accountJson.getAsString();
						if(rateJson!=null)
							rate = Long.parseLong(rateJson.getAsString());
						
						// set this as the accepted last TxId
						lastTxId = tx.getId().getSignedLongId();
						
						// done, only the more recent (2 confirmations) matters
						break;
					}
					catch (Exception e) {
						// we ignore invalid messages
					}
				}
			}
		}
	}
}
