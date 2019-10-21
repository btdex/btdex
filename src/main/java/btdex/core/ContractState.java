package btdex.core;

import java.util.HashMap;

import bt.BT;
import bt.Contract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;

public class ContractState {
	
	BurstAddress address;
	AT at;
	BurstValue balance;
	
	long arbitrator1;
	long arbitrator2;
	long offerType;
	
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

	public long getArbitrator1() {
		return arbitrator1;
	}

	public long getArbitrator2() {
		return arbitrator2;
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
	 */
	public static void addContracts(HashMap<BurstAddress, ContractState> map){
		Globals g = Globals.getInstance();
		
		BurstAddress[] atIDs = g.getNS().getAtIds().blockingGet();
		
		// reverse order to get the more recent ones first
		for (int i = atIDs.length-1; i >= 0; i--) {
			BurstAddress ad = atIDs[i];
			
			// If the map already have this one stop, since they come in order
			if(map.containsKey(ad))
				return;
			
			AT at = g.getNS().getAt(ad).blockingGet();
			
			if(checkContract(at)){
				// machine code should match perfectly
				ContractState s = new ContractState();
				
				s.updateState(at);
				map.put(ad, s);
			}
		}
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
		this.arbitrator1 = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("arbitrator1"));
		this.arbitrator2 = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("arbitrator2"));
		this.state = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("state"));
		this.rate = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("rate"));
		this.amount = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("amount"));
		this.security = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("security"));
	}

}
