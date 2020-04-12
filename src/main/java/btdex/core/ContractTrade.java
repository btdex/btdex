package btdex.core;

import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstTimestamp;

public class ContractTrade {
	private ContractState contract;
	private BurstTimestamp timestamp;
	private BurstAddress taker;
	
	private long rate;
	private long security;
	private long amount;
	private long market;
	
	public ContractTrade(ContractState contract, BurstTimestamp timestamp, BurstAddress taker,
			long rate, long security, long amount, long market) {
		super();
		this.contract = contract;
		this.timestamp = timestamp;
		this.taker = taker;
		this.rate = rate;
		this.security = security;
		this.amount = amount;
		this.market = market;
	}
	
	public ContractState getContract() {
		return contract;
	}
	public BurstTimestamp getTimestamp() {
		return timestamp;
	}
	public BurstAddress getCreator() {
		return contract.getCreator();
	}
	public BurstAddress getTaker() {
		return taker;
	}
	public long getRate() {
		return rate;
	}
	public long getAmount() {
		return amount;
	}
	public long getSecurity() {
		return security;
	}
	public long getMarket() {
		return market;
	}
	
}
