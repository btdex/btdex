package btdex.sc;

import bt.Address;
import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * BTDEX fee management contract.
 * 
 * This contract manages the TRT "minting" to offer makers and to the DAO contract as well
 * as the automatic distribution of the trade fees to TRT token holders.
 * 
 * Offer makers receive their TRT as soon as this contract receives the fees from the trade contract.
 * 
 * The fees are distributed to TRT token holders as soon as a minimum amount of 1000 SIGNA is reached. 
 * Similarly, the DAO contract only get the TRT when this limit is reached, to save on fees and node effort.
 * 
 * All token holders with more than 100 TRT receive their share of the trading fees in SIGNA.
 * In case the number of token holders with this minimum amount gets over 10k holders, this minimum is
 * doubled (starts at 10 TRT). Similarly, the minimum holdings requirement is reduced if the number of qualified
 * holders is smaller than 200. So the contract adjusts itself to avoid spending too much in fees and the
 * same time distributing to a fair number of holders.
 * 
 * @author jjos
 *
 */
public class FeeContract extends Contract {
	
	long tokenId;
	Address DAO;
	long minimumHolding = 10_0000L;
	
	// temporary variables
	Address offerMaker;
	long tokenAmount;
	long tokenAmountPendingDAO;
	int numberOfHolders;

	// constants
	static final long MIN_AMOUNT_TO_DISTRIBUTE = 1_000 * ONE_SIGNA;
	static final long MAX_HOLDERS_TO_DISTRIBUTE = 10_000;
	static final long MIN_HOLDERS_TO_DISTRIBUTE = 200;
	static final long TOKEN_FACTOR = 10_000;
	
	final long ZERO = 0;
	final long TWO = 2;
	
	@Override
	public void txReceived() {
		offerMaker = getCreator(getCurrentTxSender());
		if(offerMaker.getId() == ZERO) {
			// if the sender is not a contract, we do not send the tokens out
			return;
		}
		
		// Conversion factor to adjust the decimal places
		tokenAmount = getCurrentTxAmount() / TOKEN_FACTOR;
		
		if (tokenAmount > ZERO) {
			// Offer makers and the DAO receive the minted TRT, here we send out to offer makers
			sendAmount(tokenId, tokenAmount, offerMaker);
			tokenAmountPendingDAO += tokenAmount;
		}
	}
	
	public void upgradeDAO(Address newDAO) {
		if(getCurrentTxSender().equals(DAO)) {
			// only the current DAO contract can tell us to switch to a new one
			DAO = newDAO;
		}
	}
	
	@Override
	protected void blockFinished() {
		if (getCurrentBalance() > MIN_AMOUNT_TO_DISTRIBUTE) {
			// The TRT for the DAO is accumulated and sent here to reduce the number of transactions
			sendAmount(tokenId, tokenAmountPendingDAO, DAO);
			tokenAmountPendingDAO = ZERO;
			
			numberOfHolders = getAssetHoldersCount(tokenId, minimumHolding);
			if(numberOfHolders > MAX_HOLDERS_TO_DISTRIBUTE) {
				// too many holders, let's increase the minimum to qualify
				minimumHolding *= TWO;
			}
			else if(minimumHolding > TOKEN_FACTOR && numberOfHolders < MIN_HOLDERS_TO_DISTRIBUTE) {
				// too few qualified holders, let's decrease the minimum
				minimumHolding /= TWO;
			}
			
			// Execute the SIGNA distribution to token holders
			distributeToHolders(tokenId, minimumHolding, getCurrentBalance(), ZERO, ZERO);
		}
	}
	
	public static void main(String[] args) throws Exception {
		BT.activateSIP37(true);
		
		new EmulatorWindow(FeeContract.class);
	}
	
}
