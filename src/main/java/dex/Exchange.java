package dex;

import bt.Contract;
import bt.Address;
import bt.Timestamp;
import bt.ui.EmulatorWindow;

/**
 * Decentralized exchange smart contract.
 * 
 * @author jjos
 */
public class Exchange extends Contract {

	public static final long ACTIVATION_FEE = 26 * ONE_BURST;

	public static final long STATE_FINISHED = 0x0000000000000000;
	public static final long STATE_OPEN = 0x0000000000000001;
	public static final long STATE_PAUSED = 0x0000000000000002;

	public static final long STATE_TAKEN = 0x0000000000000010;
	public static final long STATE_WAITING_PAYMT = 0x0000000000000020;
	public static final long STATE_PAYMT_REPORTED = 0x0000000000000030;

	public static final long STATE_DISPUTE = 0x0000000000000100;
	public static final long STATE_CREATOR_DISPUTE = 0x0000000000000200;
	public static final long STATE_BUYER_DISPUTE = 0x0000000000000300;

	public static final long STATE_MOD = 0x000000000000ffff;

	Address arbitrator1;
	Address arbitrator2;
	long offerType;

	long state;
	long rate;
	Timestamp pauseTimeout;
	long amount;
	long security;
	
	Address taker;
	long fee;

	/* *
	 * Configure the offer, must set the arbitrator and offer type (FIAT, BTC, etc.).
	 * 
	 * This function should be called when reusing this contract for a new type
	 * of offer or to change the arbitrators.
	 * 
	 * @param arbitrator
	 * @param arbitrator2
	 * @param offerType
	 * /
	public void configureOffer(Address arbitrator, Address arbitrator2, long offerType) {
		if (getCurrentTxSender() == getCreator() && state < STATE_TAKEN) {
			// only creator can do this, and state should not be taken
			this.state = STATE_PAUSED;
			this.arbitrator = arbitrator;
			this.arbitrator2 = arbitrator2;
			this.offerType = offerType;
		}
	} */

	/**
	 * Update the offer settings (if creator) or take the offer (if someone else).
	 * 
	 * With this method the creator can update the rate, security deposit, and pause
	 * timeout. If the method is called by someone else, it takes the offer.
	 * 
	 * The offer status is also changed to **open** if the given timeout is greater
	 * than zero. If the cretor sets the security deposit to zero, the offer is
	 * withdrawn and all balance is send back.
	 * 
	 * @param rate
	 * @param pauseTimeout number of minutes this order should be open
	 * @param security the security deposit of this offer (in planck)
	 */
	public void updateOrTake(long rate, long security, long pauseTimeout) {
		if (getCurrentTxSender() == getCreator()) {
			// is the creator, so setting the options
			if (state < STATE_TAKEN) {
				// the order should not be taken for us to change it
				this.state = STATE_PAUSED;
				this.rate = rate;
				this.security = security;
				if (security == 0) {
					// withdraw, taking any security deposit balance
					sendBalance(getCreator());
					return;
				}
				this.pauseTimeout = getCurrentTxTimestamp().addMinutes(pauseTimeout);
				if (pauseTimeout > 0)
					this.state = STATE_OPEN;

				// Amount being sold is the current balance.
				// Seller can loose this deposit if not respecting the protocol
				amount = getCurrentBalance();
			}
		} else {
			// someone else trying to take the order
			if (getCurrentTxTimestamp().ge(this.pauseTimeout)) {
				// offer has timed out
				state = STATE_PAUSED;
			}
			if (state != STATE_OPEN || this.rate != rate || this.security != security
					|| getCurrentTxAmount() < security) {
				// if it is not open or values do no match, refund
				sendAmount(getCurrentTxAmount(), getCurrentTxSender());
				return;
			}

			// all is OK, let's take this offer
			state = STATE_WAITING_PAYMT;
			taker = getCurrentTxSender();
		}
	}

	/**
	 * Report that the payment was completed (if taker) or that payment was received
	 * (if creator).
	 */
	public void reportComplete() {
		if (getCurrentTxSender() == taker && state == STATE_WAITING_PAYMT) {
			state = STATE_PAYMT_REPORTED;
		}
		if (getCurrentTxSender() == getCreator() && state >= STATE_WAITING_PAYMT) {
			// Transfer the funds and finish this contract
			fee = 2 * amount / 1000; // fee of 0.2 % for each side
			sendAmount(fee, arbitrator1);
			sendAmount(fee, arbitrator2);

			// Send to the buyer the amount, plus his security deposit, minus fee
			state = STATE_FINISHED;
			sendBalance(taker);
			taker = null; // actually executed only when reactivated (got some more balance)
		}
	}

	/**
	 * Opens a dispute by taker or creator.
	 */
	public void openDispute() {
		if (state >= STATE_WAITING_PAYMT && getCurrentTxSender() == getCreator()) {
			state = STATE_CREATOR_DISPUTE;
			return;
		}
		if (state >= STATE_WAITING_PAYMT && getCurrentTxSender() == taker) {
			state = STATE_BUYER_DISPUTE;
		}
	}

	/**
	 * Closes the dispute sending the given amounts to creator and taker and the
	 * rest taken as dispute fee.
	 * 
	 * @param amountToCreator
	 * @param amountToTaker
	 */
	public void closeDispute(long amountToCreator, long amountToTaker) {
		if (state >= STATE_DISPUTE && (getCurrentTxSender() == arbitrator1 || getCurrentTxSender() == arbitrator2)) {
			sendAmount(amountToCreator, getCreator());
			sendAmount(amountToTaker, taker);
			state = STATE_FINISHED;
			// dispute fee is sent to the arbitrator
			sendBalance(getCurrentTxSender());
		}
	}

	@Override
	public void txReceived() {
		// We ignore any messages other than the above functions, refund
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}

	public static void main(String[] args) {
		new EmulatorWindow(Exchange.class);
	}
}
