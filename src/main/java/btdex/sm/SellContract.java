package btdex.sm;

import bt.Address;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * BlockTalk on-chain decentralized exchange (BTDEX) smart contract for selling BURST.
 * 
 * Someone willing to sell BURST should create a contract instance and configure
 * it accordingly. There is zero fee for those selling BURST and 0.25% fee for
 * the buyer (taker). Smart contract activation fees as well as transaction
 * network (mining) fees also apply.
 * 
 * Occasional trade disputes are handled by a mediation system. Fees go to a
 * smart contract account to be distributed among TRT holders monthly.
 * TRT, in turn, is distributed as trade rewards, for trade offer makers
 * and mediators.
 * 
 * @author jjos
 */
public class SellContract extends Contract {

	public static final long ACTIVATION_FEE = 24 * ONE_BURST;

	public static final long STATE_FINISHED = 0x0000000000000000;
	public static final long STATE_OPEN = 0x0000000000000001;

	public static final long STATE_TAKEN = 0x0000000000000010;
	public static final long STATE_WAITING_PAYMT = 0x0000000000000020;
	public static final long STATE_PAYMT_REPORTED = 0x0000000000000040;

	public static final long STATE_DISPUTE = 0x0000000000000100;
	public static final long STATE_CREATOR_DISPUTE = 0x0000000000000200;
	public static final long STATE_TAKER_DISPUTE = 0x0000000000000400;
	
	public static final long STATE_BOTH_DISPUTE = STATE_CREATOR_DISPUTE|STATE_TAKER_DISPUTE;

	/** Address of the contract collecting the fees to be distributed among TRT holders */
	Address feeContract;
	
	Address mediator1;
	Address mediator2;
	long offerType;
	long accountHash;

	long state;
	long rate;
	long amount;
	long security;

	Address taker;
	long fee;
	
	long disputeCreatorAmountToCreator;
	long disputeCreatorAmountToTaker;
	long disputeTakerAmountToCreator;
	long disputeTakerAmountToTaker;

	/**
	 * Update the offer settings, must be called by the creator.
	 * 
	 * With this method the creator can update the rate, security deposit.
	 * The offer status is also changed to **open** if the given security
	 * deposit is greater than zero. If the creator sets the security
	 * deposit as zero, the offer is withdrawn and all balance is sent back
	 * to the creator.
	 * 
	 * @param rate         the exchange rate per BURST
	 * @param security     the security deposit of this offer (in planck)
	 */
	public void update(long rate, long security) {
		if (getCurrentTxSender() == getCreator() && state < STATE_TAKEN) {
			// is the creator and the order should not be taken for us to change it
			this.rate = rate;
			this.security = security;
			this.state = STATE_OPEN;
			if (security == 0) {
				// withdraw, taking any security deposit balance
				amount = 0;
				state = STATE_FINISHED;
				sendBalance(getCreator());
				return;
			}
			
			// Amount being sold is the current balance, minus the security.
			// Seller can loose the security deposit if not respecting the protocol
			amount = getCurrentBalance() - security;
			// 0.25% fee
			fee = amount/400;
		}
	}

	/**
	 * Take an open offer by sending the expected security deposit.
	 * 
	 * The order is actually taken only if the given rate and security values match
	 * those currently valid for this contract. Besides, the amount along with
	 * this transaction should be higher or equal the security deposit required.
	 * If any of these conditions is not met, the order is not taken and the
	 * amount sent is refunded (minus the activation fee).
	 * 
	 * @param rate         the exchange rate per BURST
	 * @param security     the security deposit of this offer (in planck)
	 * @param amount       the amount of this offer (in planck)
	 */
	public void take(long rate, long security, long amount) {
		if (state != STATE_OPEN || this.rate != rate || this.security != security
				|| this.amount != amount
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

	/**
	 * Report that the payment was received and then send the BURST amount to taker.
	 * 
	 * This method also return the respective security deposits as well as pays the
	 * fee.
	 */
	public void reportComplete() {
		if (getCurrentTxSender() == getCreator() && state >= STATE_WAITING_PAYMT) {
			// Only the creator can report the process is complete

			// creator gets back the security deposit (zero fee)
			sendAmount(security, getCreator());

			// taker gets the amount plus his security minus fee
			sendAmount(amount + security - fee, taker);
			taker = null;
			state = STATE_FINISHED;

			// the fee comes from the balance
			sendBalance(feeContract);
		}
	}

	/**
	 * Opens or updates a dispute either by taker or creator.
	 * 
	 * If creator and taker agree on the amounts, the trade is finished. Both creator
	 * and taker can call this method multiple times until agreement is reached.
	 * 
	 * If there is no agreement, a mediator have to intervene.
	 * 
	 */
	public void dispute(long amountToCreator, long amountToTaker) {
		if (state >= STATE_WAITING_PAYMT && getCurrentTxSender() == getCreator()) {
			disputeCreatorAmountToCreator = amountToCreator;
			disputeCreatorAmountToTaker = amountToTaker;
			
			state |= STATE_CREATOR_DISPUTE;
		}
		if (state >= STATE_WAITING_PAYMT && getCurrentTxSender() == taker) {
			disputeTakerAmountToCreator = amountToCreator;
			disputeTakerAmountToTaker = amountToTaker;

			state |= STATE_TAKER_DISPUTE;
		}
		
		// check if there is agreement or is the mediator
		if((state >= STATE_DISPUTE && (getCurrentTxSender() == mediator1 || getCurrentTxSender() == mediator2)) ||
				((state & STATE_BOTH_DISPUTE) == STATE_BOTH_DISPUTE &&
				disputeCreatorAmountToCreator == disputeTakerAmountToCreator &&
				disputeCreatorAmountToTaker == disputeTakerAmountToTaker)) {
			// there was agreement or is the mediator
			if(amountToCreator + amountToTaker > amount + security + security - fee) {
				// only pay if the amounts are valid, if they collude to use tampered
				// clients with invalid amounts they don't receive their deposits
				sendAmount(amountToCreator, getCreator());
				sendAmount(amountToTaker, taker);
			}
			taker = null;
			state = STATE_FINISHED;

			// balance goes to the fee contract
			sendBalance(feeContract);
		}
	}

	@Override
	public void txReceived() {
		// We ignore any messages other than the above functions
	}

	public static void main(String[] args) {
		new EmulatorWindow(SellContract.class);
	}
}
