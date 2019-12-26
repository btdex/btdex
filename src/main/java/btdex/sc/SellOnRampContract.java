package btdex.sc;

import bt.Address;
import bt.Contract;
import bt.Timestamp;
import bt.ui.EmulatorWindow;

/**
 * BTDEX smart contract for on-ramp selling BURST (without buyer security deposit).
 * 
 * This is another smart contract option which is actually never *taken* by buyers,
 * requiring no security deposit from the buyer side. Most of the logic is checked
 * outside the smart contract, by checking for standard (low fee) on-chain text 
 * messages sending zero BURST.
 * 
 * Someone willing to sell BURST should create a contract instance and configure
 * it accordingly. For the on-ramp contracts there is a one time 1% fee charged
 * only on BURST withdraw. The same amount paid in BURST as fee is rewarded on
 * TRT to the contract creator and mediators.
 * 
 * Occasional trade disputes are handled by a mediation system. Fees go to a
 * smart contract account to be distributed among TRT holders monthly.
 *  
 * @author jjos
 */
public class SellOnRampContract extends Contract {

	public static final long ACTIVATION_FEE = 20 * ONE_BURST;

	public static final long STATE_OPEN = 0x0000000000000000;
	public static final long STATE_WITHDRAW_REQUESTED = 0x0000000000000002;
	public static final long STATE_DISPUTE = 0x0000000000000100;

	/** Address of the contract collecting the fees to be distributed among TRT holders */
	Address feeContract;
	
	Address mediator1;
	Address mediator2;
	long offerType;
	long accountHash;
	long lockMinutes;

	long state;
	long fee;
	
	Timestamp withdrawAvailable;
	
	public void withdraw() {
		if(getCurrentTxSender().equals(getCreator()) && state == STATE_OPEN) {
			state = STATE_WITHDRAW_REQUESTED;
			withdrawAvailable = getCurrentTxTimestamp().addMinutes(lockMinutes);
			return;
		}
		
		if(state == STATE_WITHDRAW_REQUESTED && getCurrentTxTimestamp().ge(withdrawAvailable)) {
			// Update the state so that it is available for use once more balance is added
			state = STATE_OPEN;

			// 1% fee on withdraw
			fee = getCurrentBalance()/100;
			sendAmount(fee, feeContract);
			sendBalance(getCreator());			
		}
	}

	/**
	 * Method to open/update a dispute.
	 * 
	 * For this particular contract with no security deposit, disputes are open by
	 * a mediator only.
	 * 
	 * The creator cannot withdraw funds when a dispute is open.
	 * 
	 */
	public void dispute(boolean keepUnderDispute, long amountToFee) {
		if(getCurrentTxSender().equals(mediator1) || getCurrentTxSender().equals(mediator2)) {
			// Determine the new state
			state = STATE_OPEN;
			if(keepUnderDispute)
				state = STATE_DISPUTE;
			
			if(amountToFee > 0)
				sendAmount(amountToFee, feeContract);			
		}
	}

	@Override
	public void txReceived() {
		// We ignore any messages other than the above functions
	}

	public static void main(String[] args) {
		new EmulatorWindow(SellOnRampContract.class);
	}
}
