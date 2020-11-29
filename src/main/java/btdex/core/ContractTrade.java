package btdex.core;

import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstTimestamp;
import burst.kit.entity.response.Transaction;

public class ContractTrade {
	private ContractState contract;
	private Transaction tx;
	
	private long rate;
	private long security;
	private long amount;
	private long market;
	
	public ContractTrade(ContractState contract, Transaction tx,
			long rate, long security, long amount, long market) {
		super();
		this.contract = contract;
		this.tx = tx;
		this.rate = rate;
		this.security = security;
		this.amount = amount;
		this.market = market;
	}
	
	public ContractState getContract() {
		return contract;
	}
	public BurstTimestamp getTimestamp() {
		return tx.getTimestamp();
	}
	public BurstAddress getCreator() {
		return contract.getCreator();
	}
	public BurstAddress getTaker() {
		return tx.getSender();
	}
	public BurstID getTakeID() {
		return tx.getId();
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
