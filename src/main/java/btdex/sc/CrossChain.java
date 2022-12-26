package btdex.sc;

import bt.Address;
import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * BTDEX smart contract for cross-chain trades of SIGNA and other coins.
 * 
 * A single contract can handle multiple offers and is created only once by
 * the system manager.
 * 
 * People willing to buy or sell SIGNA should interact with the contract.
 * There is zero fee for the offer makers and 0.25% fee for takers.
 * Smart contract activation fees as well as transaction network (mining) fees also apply.
 * 
 * Occasional trade disputes are handled by a mediation system controlled by the TRT-DAO.
 * 
 * Fees go to a smart contract account to be distributed among TRT holders. This contract
 * distributes the rewards at a given frequency as well as mint TRT for offer makers and
 * for the TRT-DAO.
 * 
 * @author jjos
 */
public class CrossChain extends Contract {

	public static final long ACTIVATION_FEE = 40000000L;

	public static final long TYPE_BUY = 0;
	public static final long TYPE_SELL = 1;

	public static final long STATE_FINISHED = 0x0000000000000000;
	public static final long STATE_OPEN = 0x0000000000000001;

	public static final long STATE_TAKEN = 0x0000000000000010;
	public static final long STATE_WAITING_PAYMT = 0x0000000000000020;

	public static final long STATE_DISPUTE = 0x0000000000000100;
	public static final long STATE_CREATOR_DISPUTE = 0x0000000000000200;
	public static final long STATE_TAKER_DISPUTE = 0x0000000000000400;
	
	public static final long STATE_BOTH_DISPUTE = STATE_CREATOR_DISPUTE|STATE_TAKER_DISPUTE;

	/** Address of the contract collecting the fees to be distributed among TRT holders */
	Address feeContract;
	/** Address of the TRT-DAO contract that mediates disputes. */
	Address DAOContract;

	// Map-value keys
	public static final long KEY_STATE = 1;
	public static final long KEY_TYPE = 2;
	public static final long KEY_TRADE_AMOUNT = 3;
	public static final long KEY_SECURITY = 4;
	public static final long KEY_MAKER = 5;
	public static final long KEY_TAKER = 6;
	// Map-value dispute keys
	public static final long KEY_AMOUNT_TO_MAKER_MAKER = 101;
	public static final long KEY_AMOUNT_TO_TAKER_MAKER = 102;
	public static final long KEY_AMOUNT_TO_MAKER_TAKER = 103;
	public static final long KEY_AMOUNT_TO_TAKER_TAKER = 104;
	
	// Temporary variables
	Address maker, taker;
	long state;
	long fee;
	long tradeAmount;
	long offer;
	long security;
	long requiredAmount;
	long balance;
	
	long disputeAmountToMakerOther;
	long disputeAmountToTakerOther;

	/**
	 * Cancel an open offer, must not be taken.
	 * 
	 * @param offer the offer id to cancel.
	 */
	public void cancel(long offer) {
		if (getCurrentTxSender().getId() == getMapValue(KEY_MAKER, offer) && getMapValue(KEY_STATE, offer) < STATE_TAKEN) {
			// only if is the maker and the order is not taken
			
			// we send back the security locked
			security = getMapValue(KEY_SECURITY, offer);
			if(getMapValue(KEY_TYPE, offer) == TYPE_SELL) {
				// plus the trade amount also locked in case of a SELL offer
				security += getMapValue(KEY_TRADE_AMOUNT, offer);
			}
			sendAmount(security, getCurrentTxSender());
			setMapValue(KEY_STATE, offer, STATE_FINISHED);
		}
	}
	
	/**
	 * Create a new buy SIGNA offer.
	 * 
	 * The amount sent along with the transaction is the security deposit.
	 * 
	 * @param tradeAmount the amount of SIGNA this offer wants to buy.
	 */
	public void createBuyOffer(long tradeAmount) {
		offer = getCurrentTx().getId();
		// Security is the amount sent
		setMapValue(KEY_SECURITY, offer, getCurrentTxAmount());
		setMapValue(KEY_TRADE_AMOUNT, offer, tradeAmount);
		setMapValue(KEY_TYPE, offer, TYPE_BUY);
		setMapValue(KEY_STATE, offer, STATE_OPEN);
	}

	/**
	 * Create a new sell SIGNA offer.
	 * 
	 * The amount sent along with the transaction is the sum of security deposit and trade amount.
	 * If {@link #security} is negative or higher than the amount sent in the transaction, then the offer is
	 * invalid (as the trade amount would be negative) and it is not recorded.
	 * 
	 * @param security the amount to be taken as security deposit in SIGNA.
	 */
	public void createSellOffer(long security) {
		// Amount sent is the trade amount plus the security
		tradeAmount = getCurrentTxAmount() - security;
		if(tradeAmount < 0 || security < 0) {
			// invalid offer, refund
		    sendAmount(getCurrentTxAmount(), getCurrentTxSender());
			return;
		}
		offer = getCurrentTx().getId();
		setMapValue(KEY_SECURITY, offer, security);
		setMapValue(KEY_TRADE_AMOUNT, offer, tradeAmount);
		setMapValue(KEY_TYPE, offer, TYPE_SELL);
		setMapValue(KEY_STATE, offer, STATE_OPEN);
	}

	/**
	 * Take an open offer by sending the expected amount plus security deposit.
	 * 
	 * The order is actually taken only if the security and amount values match
	 * those stored for the offer in question. Besides, the amount along with
	 * this transaction should be higher or equal the required amount (security
	 * or security + trade amount depending on the type of offer).
	 * 
	 * If any of these conditions is not met, the order is not taken and the
	 * amount sent is refunded (minus the activation fee).
	 * 
	 * @param security the security deposit of this offer (in planck)
	 * @param tradeAmount   the amount of this offer (in planck)
	 * @param offer	   the offer ID we are taking
	 */
	public void take(long security, long tradeAmount, long offer) {
		requiredAmount = security;
		if(getMapValue(KEY_TYPE, offer) == TYPE_BUY) {
			requiredAmount += tradeAmount;
		}
		
		if (getMapValue(KEY_STATE, offer) != STATE_OPEN || getMapValue(KEY_SECURITY, offer) != security
				|| getMapValue(KEY_TRADE_AMOUNT, offer) != tradeAmount
				|| getCurrentTxAmount() < requiredAmount) {
			// if it is not open or values do no match, refund
			sendAmount(getCurrentTxAmount(), getCurrentTxSender());
			return;
		}

		// All fine, let's take this offer.
		// Taker can lose his security deposit if not respecting the protocol
		setMapValue(KEY_STATE, offer, STATE_WAITING_PAYMT);
		setMapValue(KEY_TAKER, offer, getCurrentTxSender().getId());
	}	

	/**
	 * Report that the payment was received and then send the SIGNA amount and deposits accordingly.
	 * 
	 * For offers of type BUY SIGNA, only the taker can report complete since he is the one waiting
	 * for the cross-chain transfer.
	 * 
	 * For offers of type SELL SIGNA, the maker is the one that have to report the trade is complete.
	 */
	public void reportComplete(long offer) {
		state = getMapValue(KEY_STATE, offer);
		if(state < STATE_WAITING_PAYMT) {
			return;
		}
		maker = getAddress(getMapValue(KEY_MAKER, offer));
		taker = getAddress(getMapValue(KEY_TAKER, offer));
		security = getMapValue(KEY_SECURITY, offer);
		tradeAmount = getMapValue(KEY_TRADE_AMOUNT, offer);
		// 0.25% fee
		fee = tradeAmount/400;

		if(getMapValue(KEY_TYPE, offer) == TYPE_BUY) {
			if (getCurrentTxSender() == taker) {
				// Only the taker can report the process is complete in this case

				// maker gets the amount plus security (no fees)
				sendAmount(security + tradeAmount, maker);

				// taker gets his security minus fee
				sendAmount(security - fee, taker);
				state = STATE_FINISHED;
			}
		}
		else {
			if (getCurrentTxSender() == maker) {
				// Only the maker can report the process is complete in this case

				// maker gets back the security deposit (no fees)
				sendAmount(security, maker);

				// taker gets the amount plus his security minus fee
				sendAmount(tradeAmount + security - fee, taker);
				state = STATE_FINISHED;
			}
		}

		if(state == STATE_FINISHED) {
			sendAmount(fee, feeContract);
			setMapValue(KEY_STATE, offer, state);			
		}
	}

	/**
	 * Opens or updates a dispute either by taker or maker.
	 * 
	 * If maker and taker agree on the amounts, the trade is finished. Both maker
	 * and taker can call this method multiple times until agreement is reached.
	 * 
	 * If there is no agreement, the DAO contract have to intervene.
	 */
	public void dispute(long amountToMaker, long amountToTaker, long offer) {
		state = getMapValue(KEY_STATE, offer);
		if (state < STATE_WAITING_PAYMT) {
			return;
		}
		
		maker = getAddress(getMapValue(KEY_MAKER, offer));
		taker = getAddress(getMapValue(KEY_TAKER, offer));
		if(getCurrentTxSender() == maker) {
			setMapValue(KEY_AMOUNT_TO_MAKER_MAKER, offer, amountToMaker);
			setMapValue(KEY_AMOUNT_TO_TAKER_MAKER, offer, amountToTaker);

			disputeAmountToMakerOther = getMapValue(KEY_AMOUNT_TO_MAKER_TAKER, offer);
			disputeAmountToTakerOther = getMapValue(KEY_AMOUNT_TO_TAKER_TAKER, offer);

			state |= STATE_CREATOR_DISPUTE;
		}
		if (getCurrentTxSender() == taker) {
			setMapValue(KEY_AMOUNT_TO_MAKER_TAKER, offer, amountToMaker);
			setMapValue(KEY_AMOUNT_TO_TAKER_TAKER, offer, amountToTaker);

			disputeAmountToMakerOther = getMapValue(KEY_AMOUNT_TO_MAKER_MAKER, offer);
			disputeAmountToTakerOther = getMapValue(KEY_AMOUNT_TO_TAKER_MAKER, offer);

			state |= STATE_TAKER_DISPUTE;
		}
		
		// check if there is agreement or is the DAO contract
		if((state >= STATE_DISPUTE && (getCurrentTxSender() == DAOContract)) ||
				((state & STATE_BOTH_DISPUTE) == STATE_BOTH_DISPUTE &&
				disputeAmountToMakerOther == amountToMaker && disputeAmountToTakerOther == amountToTaker)) {
			// there was agreement or is the DAO contract
			
			tradeAmount = getMapValue(KEY_TRADE_AMOUNT, offer);
			security = getMapValue(KEY_SECURITY, offer);
			fee = tradeAmount / 400;

			// Send the fee
			sendAmount(fee, feeContract);

			// total balance for this trade
			balance = tradeAmount + security + security - fee;
			
			if(amountToMaker + amountToTaker <= balance) {
				// only pay if the amounts are valid, if they collude to use tampered
				// clients with invalid amounts they don't receive their deposits
				sendAmount(amountToMaker, maker);
				sendAmount(amountToTaker, taker);
				
				balance = balance - amountToMaker - amountToTaker;
			}
			state = STATE_FINISHED;

			// Occasional residual balance goes to the DAO contract
			balance = balance - fee;
			sendAmount(balance, DAOContract);
		}
		setMapValue(KEY_STATE, offer, state);
	}
	
	public void upgradeDAO(Address newDAOContract) {
		if(getCurrentTxSender() == DAOContract) {
			// only the old DAO contract can give us a new one
			DAOContract = newDAOContract;
		}
	}
	
	public void upgradeFeeContract(Address newFeeContract) {
		if(getCurrentTxSender() == DAOContract) {
			// only the old DAO contract can give us a new one
			feeContract = newFeeContract;
		}
	}

	@Override
	public void txReceived() {
		// We ignore any messages other than the above functions
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		
		new EmulatorWindow(CrossChain.class);
	}
}
