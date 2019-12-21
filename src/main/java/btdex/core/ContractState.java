package btdex.core;

import java.util.HashMap;

import bt.BT;
import bt.Contract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;

public class ContractState {
	
	BurstAddress address;
	AT at;
	BurstValue balance;
	
	long mediator1;
	long mediator2;
	long offerType;
	long feeContract;
	
	long state;
	long rate;
	long amount;
	long security;
	
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
		BurstID idLimit = BurstID.fromLong("9601465021860021685");
		// FIXME: add a mainnet limit
		
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
			
			if(checkContract(at)){
				// machine code should match perfectly
				ContractState s = new ContractState();
				
				s.updateState(at);
				map.put(ad, s);
			}
		}
		
		return first;
	}
	
	public static boolean checkContract(AT at) {
		byte []code = Globals.getInstance().getContract().getCode();
		
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
		if(at == null)
			at = Globals.getInstance().getNS().getAt(address).blockingGet();
		
		this.at = at;
		this.address = at.getId();
		this.balance = at.getBalance();
		
		this.offerType = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("offerType"));
		this.mediator1 = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("mediator1"));
		this.mediator2 = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("mediator2"));
		this.state = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("state"));
		this.rate = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("rate"));
		this.amount = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("amount"));
		this.security = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("security"));
		this.feeContract = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("feeContract"));
	}

}
