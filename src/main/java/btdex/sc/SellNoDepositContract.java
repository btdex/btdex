package btdex.sc;

import bt.Address;
import bt.Contract;
import bt.Timestamp;
import bt.ui.EmulatorWindow;

/**
 * BTDEX special no-deposit smart contract.
 * 
 * This is another smart contract option which is actually never *taken* by buyers,
 * requiring no security deposit from the buyer side. Most of the logic is checked
 * outside the smart contract, by checking for standard (low fee) on-chain text 
 * messages sending zero BURST.
 * 
 * Someone willing to sell BURST should create a contract instance and configure
 * it accordingly. For these no-deposit contracts there is a one time 1% fee charged
 * only on BURST withdraw. The same amount paid in BURST as fee is rewarded on
 * TRT to the contract creator and mediators.
 * 
 * Occasional trade disputes are handled by a mediation system. Fees go to a
 * smart contract account to be distributed among TRT holders monthly.
 *  
 * @author jjos
 */
public class SellNoDepositContract extends Contract {

	public static final long ACTIVATION_FEE = 20 * ONE_BURST;

	public static final long STATE_FINISHED = 0x0000000000000000;
	public static final long STATE_OPEN = 0x0000000000000001;
	public static final long STATE_WITHDRAW_REQUESTED = 0x0000000000000002;
	public static final long STATE_DISPUTE = 0x0000000000000100;

	/** Address of the contract collecting the fees to be distributed among TRT holders */
	Address feeContract;
	
	Address mediator1;
	Address mediator2;
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
	 * For this particular contract with no buyer security deposit, disputes are open by
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
		// The creator opens the contract by sending an amount without any message
		if(state == STATE_FINISHED && getCurrentTxSender().equals(getCreator())) {
			state = STATE_OPEN;
		}
	}

	public static void main(String[] args) {
		new EmulatorWindow(SellNoDepositContract.class);
	}
}
