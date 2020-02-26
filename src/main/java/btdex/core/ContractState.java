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
	
	BurstAddress address;
	Type type;
	AT at;
	BurstValue balance;
	
	long mediator1;
	long mediator2;
	long offerType;
	long feeContract;
	
	long state;
	long amount;
	long security;	
	long lockMinutes;
	
	long rate;
	int market;
	String account;
	
	long lastTxId;
	
	public ContractState(Type type) {
		this.type = type;
	}
	
	public static String format(long valueNQT) {
		double dvalue = (double)valueNQT / Contract.ONE_BURST;
		return Globals.NF.format(dvalue);
	}
	
	public long getMarket() {
		return offerType & Market.MARKET_MASK;
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
	
	public String getAmount() {
		double dvalue = (double)amount / Contract.ONE_BURST;
		return Globals.NF.format(dvalue);
	}

	public long getSecurityNQT() {
		return security;
	}
	
	public String getSecurity() {
		double dvalue = (double)security / Contract.ONE_BURST;
		return Globals.NF.format(dvalue);
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
			if(checkContractCode(at))
				type = Type.Standard;
			else if(checkContractCodeNoDeposit(at))
				type = Type.NoDeposit;
			
			if(type!=Type.Invalid) {
				ContractState s = new ContractState(type);
				
				s.updateState(at);
				map.put(ad, s);
			}
		}
		
		return first;
	}
	
	static boolean checkContractCode(AT at) {
		byte []code = Globals.getInstance().getContractCode();
		
		if(at.getMachineCode().length < code.length)
			return false;
		
		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i])
				return false;
		}
		return true;
	}
	
	static boolean checkContractCodeNoDeposit(AT at) {
		byte []code = Globals.getInstance().getContractNoDepositCode();
		
		if(at.getMachineCode().length < code.length)
			return false;
		
		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i])
				return false;
		}
		return true;
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
		
		if(type == Type.Standard) {
			Compiler contract = g.getContract();
			this.mediator1 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator1"));
			this.mediator2 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator2"));
			this.state = BT.getContractFieldValue(at, contract.getFieldAddress("state"));
			this.amount = BT.getContractFieldValue(at, contract.getFieldAddress("amount"));
			this.security = BT.getContractFieldValue(at, contract.getFieldAddress("security"));
			this.feeContract = BT.getContractFieldValue(at, contract.getFieldAddress("feeContract"));
		}
		else if(type == Type.NoDeposit) {
			Compiler contract = g.getContractNoDeposit();
			this.mediator1 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator1"));
			this.mediator2 = BT.getContractFieldValue(at, contract.getFieldAddress("mediator2"));
			this.state = BT.getContractFieldValue(at, contract.getFieldAddress("state"));
			this.lockMinutes = BT.getContractFieldValue(at, contract.getFieldAddress("lockMinutes"));
			this.feeContract = BT.getContractFieldValue(at, contract.getFieldAddress("feeContract"));
		}
		
		// check rate, type, etc. from transaction
		Transaction[] txs = g.getNS().getAccountTransactions(this.address).blockingGet();
		for(Transaction tx : txs) {
			if(tx.getId().getSignedLongId() == lastTxId)
				break;
			
			// we only accept configurations with 2 confirmations or more
			if(tx.getConfirmations()<2)
				continue;
			
			// order configurations should be set by the creator
			if(tx.getSender().getSignedLongId() != at.getCreator().getSignedLongId())
				continue;
			
			if(tx.getRecipient().getSignedLongId() == address.getSignedLongId()
					&& tx.getSender().getBurstID().getSignedLongId() == at.getCreator().getSignedLongId()
					&& tx.getType() == 2 /* TYPE_MESSAGING */
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
						if(marketJson!=null)
							market = Integer.parseInt(marketJson.getAsString());
						if(accountJson!=null)
							account = accountJson.getAsString();
						if(rateJson!=null)
							rate = Long.parseLong(rateJson.getAsString());
					}
					catch (Exception e) {
						// we ignore invalid messages
					}
				}
			}
			lastTxId = tx.getId().getSignedLongId();
		}
	}
}
