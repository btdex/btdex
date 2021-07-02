package btdex.core;

import signumj.entity.SignumAddress;
import signumj.entity.SignumID;
import signumj.entity.SignumTimestamp;
import signumj.entity.response.Transaction;

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
	public SignumTimestamp getTimestamp() {
		return tx.getTimestamp();
	}
	public SignumAddress getCreator() {
		return contract.getCreator();
	}
	public SignumAddress getTaker() {
		return tx.getSender();
	}
	public SignumID getTakeID() {
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
