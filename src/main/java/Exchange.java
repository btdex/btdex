
import bt.Contract;
import bt.Address;
import bt.Timestamp;

/**
 * Decentralized exchange smart contract.
 * 
 * @author jjos
 */
public class Exchange extends Contract {
	
	static final long STATE_FINISHED       = 0x0000000000000000;
	static final long STATE_OPEN           = 0x0000000000000001;
	static final long STATE_PAUSED         = 0x0000000000000002;

	static final long STATE_TAKEN          = 0x0000000000000010;
	static final long STATE_WAITING_PAYMT  = 0x0000000000000020;
	static final long STATE_PAYMT_REPORTED = 0x0000000000000030;

	static final long STATE_DISPUTE        = 0x0000000000000100;
	static final long STATE_CREATOR_DISPUTE= 0x0000000000000200;
	static final long STATE_BUYER_DISPUTE  = 0x0000000000000300;

	static final long STATE_MOD            = 0x000000000000ffff;
	
	Address creator;
	Address buyer;
	Address arbitrator;
	Address arbitrator2;
	Timestamp pauseTimeout;
	
	long state;
	long offerType;
	long rate;
	long amount;
	long collateral;
	
	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Exchange(){
		creator = getCreator();
	}
	
	/**
	 * Configure the offer, must set the arbitrator and offer type (FIAT, BTC, etc.)
	 * @param arbitrator
	 * @param offerType
	 */
	public void configureOffer(Address arbitrator, Address arbitrator2, long offerType) {
		if(getCurrentTx().getSenderAddress()!=creator || state>STATE_TAKEN) {
			// only creator can do this, and state should not be taken
			return;
		}
		
		this.state = STATE_PAUSED;
		this.arbitrator = arbitrator;
		this.arbitrator2 = arbitrator2;
		this.offerType = offerType;
	}
	
	/**
	 * Withdraw this offer, all funds go back to creator.
	 * 
	 * Offer must not be currently being processed (taken).
	 */
	public void withdraw() {
		if(getCurrentTx().getSenderAddress()!=creator || state>STATE_TAKEN) {
			// only creator can do this, and it should not be taken
			return;
		}
		
		this.state = STATE_FINISHED;
		sendBalance(creator);
	}
	
	/**
	 * Sets the rate for this offer, a new pause timeout, and the collateral requested.
	 * 
	 * The offer status is also changed to opened if the given timeout
	 * is greater than zero.
	 * 
	 * @param rate
	 * @param pauseTimeout
	 * @param collateral 
	 */
	public void setParameters(long rate, long pauseTimeout, long collateral) {
		if(getCurrentTx().getSenderAddress()!=creator || state>STATE_TAKEN) {
			// only creator can do this, and it should not be taken
			return;
		}
		
		this.state = STATE_PAUSED;
		this.rate = rate;
		this.collateral = collateral;
		this.pauseTimeout = getBlockTimestamp().addMinutes(pauseTimeout);
		if(pauseTimeout > 0)
			this.state = STATE_OPEN;

		// Amount being sold is the current balance minus a collateral.
		// Seller can loose this collateral if not respecting the protocol
		amount = getCurrentBalance() - collateral;
	}
	
	public void takeOffer(long rate, long collateral) {
		if(getCurrentTx().getTimestamp().ge(pauseTimeout)) {
			// offer has timed out
			state = STATE_PAUSED;
		}
		if(state != STATE_OPEN || this.rate!=rate || this.collateral!=collateral ||
			getCurrentTx().getAmount()<collateral) {
			// if it is not open or values do no match
			refund();
			return;
		}

		// all is OK, let's take this offer
		state = STATE_WAITING_PAYMT;
		buyer = getCurrentTx().getSenderAddress();
	}
	
	public void reportPaid() {
		if(state<STATE_WAITING_PAYMT || getCurrentTx().getSenderAddress()!=buyer) {
			return;
		}
		
		state = STATE_PAYMT_REPORTED;
	}
	
	public void acknowledgePayment() {
		if(state<STATE_WAITING_PAYMT || getCurrentTx().getSenderAddress()!=creator) {
			return;
		}
		
		// Transfer the funds and finish this contract (can be reopened latter)
		// arbitrator share
		long arbitratorAmount = amount * 5;
		arbitratorAmount /= 1000; // half a percent
		sendAmount(arbitratorAmount, arbitrator);
		
		// Send to the buyer the amount, plus his collateral
		sendAmount(amount+collateral , buyer);

		// Send to creator his collateral, discounted arbitrator fee
		sendBalance(creator);
		state = STATE_FINISHED;
	}
	
	public void openDispute() {
		// TODO: improve the dispute mechanism, only open after the timeout, etc.		
		if(state>=STATE_WAITING_PAYMT && getCurrentTx().getSenderAddress()==creator) {
			state = STATE_CREATOR_DISPUTE;
			sendMessage("Dispute", arbitrator);
			sendMessage("Dispute", arbitrator2);
			return;
		}
		if(state>=STATE_WAITING_PAYMT && getCurrentTx().getSenderAddress()==buyer) {
			state = STATE_BUYER_DISPUTE;
			sendMessage("Dispute", arbitrator);
			sendMessage("Dispute", arbitrator2);
			return;
		}
	}
	
	/**
	 * Closes the dispute sending the given amounts to creator and buyer and
	 * the rest to the arbitrator.
	 * 
	 * @param amountToCreator
	 * @param amountToBuyer
	 */
	public void closeDispute(long amountToCreator, long amountToBuyer) {
		// TODO: improve the dispute mechanism
		if(state>STATE_DISPUTE &&
				(getCurrentTx().getSenderAddress()==arbitrator ||
					getCurrentTx().getSenderAddress()==arbitrator2)) {
			sendAmount(amountToCreator, creator);
			sendAmount(amountToBuyer, buyer);
			sendBalance(arbitrator);
			state = STATE_FINISHED;
		}
	}
	
	void refund() {
		// send back the funds we just received
		sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
	}

	@Override
	public void txReceived() {
		// Unrecognized function call
		refund();
	}
}


