package dex;

import bt.Contract;
import bt.Address;
import bt.Timestamp;
import bt.ui.EmulatorWindow;

/**
 * An on-chain decentralized exchange smart contract.
 * 
 * Someone willing to sell BURST should create a contract instance and configure
 * it accordingly. There is a 0.1% fee for those selling BURST and 0.3% fee for
 * the buyer.Smart contract activation fees as well as transaction network fees
 * also apply.
 * 
 * Trade disputes are handled by an arbitrator system.
 * 
 * @author jjos
 */
public class Exchange extends Contract {

	public static final long ACTIVATION_FEE = 27 * ONE_BURST;

	public static final long STATE_FINISHED = 0x0000000000000000;
	public static final long STATE_OPEN = 0x0000000000000001;
	public static final long STATE_PAUSED = 0x0000000000000002;

	public static final long STATE_TAKEN = 0x0000000000000010;
	public static final long STATE_WAITING_PAYMT = 0x0000000000000020;
	public static final long STATE_PAYMT_REPORTED = 0x0000000000000040;

	public static final long STATE_DISPUTE = 0x0000000000000100;
	public static final long STATE_CREATOR_DISPUTE = 0x0000000000000200;
	public static final long STATE_TAKER_DISPUTE = 0x0000000000000400;

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

	/**
	 * Update the offer settings (if creator) or take the offer (if someone else).
	 * 
	 * With this method the creator can update the rate, security deposit, and pause
	 * timeout. The offer status is also changed to **open** if the given timeout is
	 * greater than zero. If the cretor sets the security deposit to zero, the offer
	 * is withdrawn and all balance is send back.
	 * 
	 * If the method is called by someone else, it tries to take the offer.
	 * 
	 * @param rate         the exchange rate per BURST
	 * @param security     the security deposit of this offer (in planck)
	 * @param pauseTimeout number of minutes this order should be open
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
					amount = 0;
					sendBalance(getCreator());
					return;
				}
				this.pauseTimeout = getCurrentTxTimestamp().addMinutes(pauseTimeout);
				if (pauseTimeout > 0)
					this.state = STATE_OPEN;

				// Amount being sold is the current balance, minus the security.
				// Seller can loose the security deposit if not respecting the protocol
				amount = getCurrentBalance() - security;
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

			// All fine, let's take this offer.
			// Taker can loose his security deposit if not respecting the protocol
			state = STATE_WAITING_PAYMT;
			taker = getCurrentTxSender();
		}
	}

	/**
	 * Report that the payment was received and then send the BURST amount to taker.
	 * 
	 * This method also return the respective secutiry deposits as well as pays the
	 * arbitration fees.
	 */
	public void reportComplete() {
		if (getCurrentTxSender() == getCreator() && state >= STATE_WAITING_PAYMT) {
			// Transfer the funds and finish this contract

			// fee of 0.1 % from creator
			fee = amount / 1000;
			// creator gets his security minus the fee
			sendAmount(security - fee, getCreator());

			// 0.2 % for one arbitrator
			fee += fee;
			// fees go to the arbitrators
			sendAmount(fee, arbitrator1);

			// fee of 0.3 % for taker
			fee += fee;
			// taker gets the amount plus his security minus the fee
			sendAmount(amount + security - fee, taker);
			taker = null;
			state = STATE_FINISHED;

			// the arbitrator2 fee comes from the balance
			sendBalance(arbitrator2);
		}
	}

	/**
	 * Opens a dispute by taker or creator.
	 */
	public void openDispute() {
		if (state >= STATE_WAITING_PAYMT && getCurrentTxSender() == getCreator()) {
			state += STATE_CREATOR_DISPUTE;
			return;
		}
		if (state >= STATE_WAITING_PAYMT && getCurrentTxSender() == taker) {
			state += STATE_TAKER_DISPUTE;
		}
	}

	/**
	 * Closes the dispute sending the given amounts to creator and taker and the
	 * rest taken as dispute fee.
	 * 
	 * Only an arbitrator of this contract can close a dispute.
	 * 
	 * @param amountToCreator
	 * @param amountToTaker
	 */
	public void closeDispute(long amountToCreator, long amountToTaker) {
		if (state >= STATE_DISPUTE && (getCurrentTxSender() == arbitrator1 || getCurrentTxSender() == arbitrator2)) {
			sendAmount(amountToCreator, getCreator());
			sendAmount(amountToTaker, taker);
			state = STATE_FINISHED;

			// dispute fee is sent to the arbitrators
			fee = getCurrentBalance() / 2;
			sendAmount(fee, arbitrator1);
			sendBalance(arbitrator2);
		}
	}

	@Override
	public void txReceived() {
		// We ignore any messages other than the above functions, refund any accidental
		// transaction to avoid complaints
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}

	public static void main(String[] args) {
		new EmulatorWindow(Exchange.class);
	}
}
