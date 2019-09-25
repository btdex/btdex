package btdex.core;

import java.util.Arrays;
import java.util.HashMap;

import bt.BT;
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
	long pauseTimeout;
	long amount;
	long security;
	
	public long getMarket() {
		return offerType & Globals.MARKET_MASK;
	}
	
	public BurstAddress getAddress() {
		return address;
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

	public long getPauseTimeout() {
		return pauseTimeout;
	}

	public long getAmount() {
		return amount;
	}

	public long getSecurity() {
		return security;
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
		
		byte []code = Globals.contract.getCode();
		BurstAddress[] atIDs = Globals.NS.getAtIds().blockingGet();
		
		// reverse order to get the more recent ones first
		for (int i = atIDs.length-1; i >= 0; i--) {
			BurstAddress ad = atIDs[i];
			
			// If the map already have this one stop, since they come in order
			if(map.containsKey(ad))
				return;
			
			AT at = Globals.NS.getAt(ad).blockingGet();
			
			if(at.getMachineCode().length > code.length &&
					Arrays.equals(at.getMachineCode(), 0, code.length, code, 0, code.length)){
				// machine code should match perfectly
				ContractState s = new ContractState();
				
				s.updateState(at);
				map.put(ad, s);
			}
		}
	}
	
	public void updateState(AT at) {
		if(at == null)
			at = Globals.NS.getAt(address).blockingGet();
		
		this.at = at;
		this.address = at.getId();
		this.balance = at.getBalance();
		
		this.offerType = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("offerType"));
		this.arbitrator1 = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("arbitrator1"));
		this.arbitrator2 = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("arbitrator2"));
		this.state = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("state"));
		this.rate = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("rate"));
		this.pauseTimeout = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("pauseTimeout"));
		this.amount = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("amount"));
		this.security = BT.getContractFieldValue(at, Globals.contract.getFieldAddress("security"));
	}

}
