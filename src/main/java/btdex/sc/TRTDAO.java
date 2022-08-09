package btdex.sc;

import bt.Address;
import bt.BT;
import bt.Contract;
import bt.Register;
import bt.ui.EmulatorWindow;

public class TRTDAO extends Contract {
	
	private static final long KEY_ADDRESS = 1;
	private static final long KEY_VOTES = 2;
	private static final long KEY_TYPE = 3;
	private static final long KEY_AMOUNT = 4;
	private static final long KEY_ASSET = 5;
	private static final long KEY_IS_MEMBER = 6;
	private static final long KEY_DEADLINE = 7;
	private static final long KEY_MEDIATE = 8;
	private static final long KEY_NEXT_MEDIATION = 9;
	
	private static final long TYPE_JOIN  = 0x02000000000000000L;
	private static final long TYPE_BAN   = 0x03000000000000000L;
	private static final long TYPE_FUND  = 0x04000000000000000L;
	private static final long TYPE_QUIT  = 0x05000000000000000L;
	private static final long TYPE_COMMAND  = 0x06000000000000000L;
	
	private static final long MEMBERSHIP_AMOUNT = 1_000_000_0000L;
	private static final long ZERO = 0L;
	private static final long ONE_WEEK_MINUTES = 7 * 24 * 60;
	private static final long SIX_HOURS_MINUTES = 6 * 60;
	
	private static final long DISPUTE_HASH = -5904028324194678009L;

	long tokenId;
	
	long totalMembers;
	long candidate;
	long votes;
	
	// temporary variables
	long id;
	long quantity;
	long previousTx;
	Register message;
	
	/**
	 * Initiate the process to become a DAO member.
	 * 
	 * To do so, this transaction must carry 1M TRT besides the activation fee in SIGNA.
	 * Present members can vote to accept the candidate with {@link #approveJoin(long)}.
	 * 
	 * The candidate can {@link #withdrawJoin(long)} to recover the TRT sent.
	 */
	public void askToJoin() {
		quantity = getCurrentTxAmount(tokenId);
		id = getCurrentTx().getId();
		if(quantity != MEMBERSHIP_AMOUNT || getMapValue(KEY_TYPE, id) != ZERO
				|| getMapValue(KEY_IS_MEMBER, getCurrentTxSender().getId()) != ZERO) {
			// invalid proposal
			sendAmount(tokenId, quantity, getCurrentTxSender());
			return;
		}
		
		setMapValue(KEY_TYPE, id, TYPE_JOIN);
		setMapValue(KEY_ADDRESS, id, getCurrentTxSender().getId());
		setMapValue(KEY_VOTES, id, ZERO);
		
		if(totalMembers < 1) {
			// this is the first member
			addNewMember(id);
		}
	}
	
	/**
	 * Cancels the {@link #askToJoin()} and get back the TRT locked.
	 * 
	 * @param id the {@link #askToJoin()} transaction ID.
	 */
	public void withdrawJoin(long id) {
		if(getMapValue(KEY_ADDRESS, id) != getCurrentTxSender().getId() || getMapValue(KEY_TYPE, id) != TYPE_JOIN) {
			// invalid
			return;
		}
		
		setMapValue(KEY_TYPE, id, ZERO);
		sendAmount(MEMBERSHIP_AMOUNT, tokenId, getCurrentTxSender());
	}
	
	/**
	 * Private method to execute the member addition.
	 * @param id the {@link #askToJoin()} transaction ID.
	 */
	private void addNewMember(long id) {
		candidate = getMapValue(KEY_ADDRESS, id);
		setMapValue(KEY_IS_MEMBER, candidate, candidate);
		setMapValue(KEY_TYPE, id, ZERO);
		totalMembers = totalMembers + 1;
	}

	/**
	 * Vote in favor of approving a {@link #askToJoin()} candidate.
	 * 
	 * @param id the {@link #askToJoin()} transaction id.
	 */
	public void approveJoin(long id) {
		if(doVote(getCurrentTxSender(), id, TYPE_JOIN) > totalMembers/2) {
			// approved
			addNewMember(id);
		}
	}

	/**
	 * Private method to execute a vote.
	 * 
	 * @param member the member voting
	 * @param id the transaction ID we are voting on
	 * @param type the type of request
	 * @return the number of votes on this matter.
	 */
	private long doVote(Address member, long id, long type) {
		if(getMapValue(KEY_TYPE, id) != type || getMapValue(KEY_IS_MEMBER, member.getId()) == ZERO
				|| getMapValue(id, member.getId()) != ZERO) {
			// wrong type, not a member, or already voted
			return ZERO;
		}
		
		votes = getMapValue(KEY_VOTES, id);
		votes = votes + 1;
		setMapValue(KEY_VOTES, id, votes);
		// store we have already voted
		setMapValue(id, member.getId(), id);
		return votes;
	}
	
	/**
	 * Initiate a ask for funding transaction.
	 * 
	 * Anyone can ask for funding but has to convince the mojority of members to approve it.
	 * The members can vote with {@link #approveFundind(long)}.
	 * 
	 * @param assetId the asset ID the DAO will send out (0 for SIGNA)
	 * @param amount the amount that will be release from the DAO
	 * @param beneficiary the account that will receive the funds if approved by the majority
	 */
	public void askForFunding(long assetId, long amount, Address beneficiary) {
		id = getCurrentTx().getId();
		setMapValue(KEY_TYPE, id, TYPE_FUND);
		setMapValue(KEY_ADDRESS, id, beneficiary.getId());
		setMapValue(KEY_ASSET, id, assetId);
		setMapValue(KEY_AMOUNT, id, amount);
		setMapValue(KEY_VOTES, id, ZERO);
	}
	
	/**
	 * Vote to approve a given funding request.
	 * 
	 * @param id the {@link #askForFunding(long, long, Address)} transaction id
	 */
	public void approveFundind(long id) {
		if(doVote(getCurrentTxSender(), id, TYPE_FUND) > totalMembers/2) {
			// approved
			candidate = getMapValue(KEY_ADDRESS, id);
			sendAmount(getMapValue(KEY_ASSET, id), getMapValue(KEY_AMOUNT, id), getAddress(candidate));
			
			setMapValue(KEY_TYPE, id, ZERO);
		}
	}
	
	/**
	 * Open a voting to ban a member.
	 * 
	 * Only another member can open this voting. If the majority of members approve the ban,
	 * then the member is banned without receiving any token/coins from this contract.
	 * 
	 * After one week, if there is no majority, the voting is finished.
	 * 
	 * @param member the member we are voting to ban
	 */
	public void askToBan(Address member) {
		if(getMapValue(KEY_IS_MEMBER, getCurrentTxSender().getId()) == ZERO
				|| getMapValue(KEY_IS_MEMBER, member.getId()) == ZERO) {
			// invalid ask
			return;
		}
		
		id = getCurrentTx().getId();
		setMapValue(KEY_TYPE, id, TYPE_BAN);
		setMapValue(KEY_ADDRESS, id, member.getId());
		setMapValue(KEY_VOTES, id, ZERO);
		setMapValue(KEY_DEADLINE, id, getCurrentTxTimestamp().addMinutes(ONE_WEEK_MINUTES).getValue());
	}
	
	/**
	 * Vote to approve the ban.
	 * 
	 * First a member have to {@link #askToBan(Address)} to initiate the process.
	 * If the matter is not approved in one week, the process is finished.
	 * 
	 * @param id the transaction if of the {@link #askToBan(Address)} we are approving.
	 */
	public void aproveBan(long id) {
		if (getCurrentTx().getTimestamp().getValue() > getMapValue(KEY_DEADLINE, id)) {
			// this ban timed out, invalid
			return;
		}
		if(doVote(getCurrentTxSender(), id, TYPE_BAN) > totalMembers/2) {
			// approved
			candidate = getMapValue(KEY_ADDRESS, id);
			if(getMapValue(KEY_IS_MEMBER, candidate) != ZERO) {
				setMapValue(KEY_IS_MEMBER, candidate, ZERO);
				totalMembers = totalMembers - 1;
			}

			setMapValue(KEY_TYPE, id, ZERO);
		}
	}
	
	/**
	 * A member can ask to quit.
	 * 
	 * There is a one week notice, after that period the member can call {@link #executeQuit(long)}
	 * to receive his/here TRT deposit back.
	 * 
	 * If this is a rogue quit, the member can still be banned before this one week notice period.
	 * 
	 */
	public void askToQuit() {
		if(getMapValue(KEY_IS_MEMBER, getCurrentTxSender().getId()) == ZERO) {
			// invalid ask
			return;
		}
		
		id = getCurrentTxSender().getId();
		setMapValue(KEY_TYPE, id, TYPE_QUIT);
		setMapValue(KEY_ADDRESS, id, getCurrentTxSender().getId());
		setMapValue(KEY_DEADLINE, id, getCurrentTxTimestamp().addMinutes(ONE_WEEK_MINUTES).getValue());
	}
	
	/**
	 * Cancel a previous ask to quit.
	 * 
	 * @param id the id of the previous ask to quit.
	 */
	public void withdrawQuit(long id) {
		if(getMapValue(KEY_TYPE, id) != TYPE_QUIT || getMapValue(KEY_ADDRESS, id) != getCurrentTxSender().getId()) {
			// invalid
			return;
		}
		
		setMapValue(KEY_TYPE, id, ZERO);
	}
	
	/**
	 * Execute quitting from the DAO, requires a previous {@link #askToQuit()}.
	 * 
	 * @param id the previous {@link #askToQuit()} transaction id
	 */
	public void executeQuit(long id) {
		if(getMapValue(KEY_TYPE, id) != TYPE_QUIT) {
			return;
		}
		candidate = getMapValue(KEY_ADDRESS, id);
		if (getMapValue(KEY_IS_MEMBER, candidate) == ZERO) {
			// invalid
			return;
		}
		if (getCurrentTx().getTimestamp().getValue() > getMapValue(KEY_DEADLINE, id)) {
			sendAmount(tokenId, MEMBERSHIP_AMOUNT, getAddress(candidate));
			setMapValue(KEY_TYPE, id, ZERO);
			setMapValue(KEY_IS_MEMBER, candidate, ZERO);
			totalMembers = totalMembers - 1;
		}
	}
	
	/**
	 * Mediate a contract dispute.
	 * 
	 * At least two members have to send the same mediate command to to the same offer to the DAO
	 * contract settle the dispute.
	 * If a member sends a different command, the previous one is erased and again a new identical
	 * command is necessary. The command being sent should be on the *second page* of the message
	 * sent to call this method.
	 * 
	 * There is a rate limit for the number of disputes a member can vote for.
	 * 
	 * @param contract the contract we are mediating
	 * @param offer the offer id being disputed
	 */
	public void mediate(Address contract, long offer) {
		if(getMapValue(KEY_IS_MEMBER, getCurrentTxSender().getId()) ==  getCurrentTxSender().getId()) {
			// sender must be a member
			
			// Limit the number of mediation approvals in a given period of time
			if(getMapValue(KEY_NEXT_MEDIATION, getCurrentTxSender().getId()) < getCurrentTxTimestamp().getValue()) {
				return;
			}

			message = getCurrentTx().getMessage(1);
			if(message.getValue1() != DISPUTE_HASH) {
				// only dispute mediation messages are accepted here
				return;
			}

			// check if there is already a previous message
			previousTx = getMapValue(KEY_MEDIATE, offer);
			if(previousTx == ZERO || message != getTransaction(previousTx).getMessage(1)) {
				// no previous suggestion, or we are making a new one
				setMapValue(KEY_MEDIATE, offer, getCurrentTx().getId());
			}
			else {
				// a second identical suggestion from a member, so we execute
				sendMessage(message, contract);
				// clear the state
				setMapValue(KEY_MEDIATE, offer, ZERO);
				
				// next mediation for this account only in six hours from now
				setMapValue(KEY_NEXT_MEDIATION, getCurrentTxSender().getId(),
						getCurrentTxTimestamp().addMinutes(SIX_HOURS_MINUTES).getValue());
			}
		}
	}
	
	/**
	 * Open a voting to send a command for a given contract.
	 * 
	 * Only a member can open this voting. If the majority of members approve the command,
	 * then the command is sent to the given contract.
	 * 
	 * This can be used to upgrade the DAO contract or other actions that require the majority.
	 * 
	 * After one week, if there is no majority, the voting is finished.
	 * 
	 * @param contract the contract we should send the command
	 */
	public void askForCommand(Address contract) {
		if(getMapValue(KEY_IS_MEMBER, getCurrentTxSender().getId()) == ZERO) {
			// only members
			return;
		}
		
		id = getCurrentTx().getId();
		setMapValue(KEY_TYPE, id, TYPE_COMMAND);
		setMapValue(KEY_ADDRESS, id, contract.getId());
		setMapValue(KEY_VOTES, id, ZERO);
		setMapValue(KEY_DEADLINE, id, getCurrentTxTimestamp().addMinutes(ONE_WEEK_MINUTES).getValue());
	}
	
	/**
	 * Vote to approve the command.
	 * 
	 * First a member have to {@link #askForCommand(Address)} to initiate the process.
	 * If the matter is not approved in one week, the process is finished.
	 * 
	 * @param id the transaction if of the {@link #askForCommand(Address)} we are approving.
	 */
	public void aproveCommand(long id) {
		if (getCurrentTx().getTimestamp().getValue() > getMapValue(KEY_DEADLINE, id)) {
			// this voting timed out, invalid
			return;
		}
		if(doVote(getCurrentTxSender(), id, TYPE_COMMAND) > totalMembers/2) {
			// approved
			sendMessage(getTransaction(id).getMessage(1),
					getAddress(getMapValue(KEY_ADDRESS, id)));
			setMapValue(KEY_TYPE, id, ZERO);
		}
	}
	
	@Override
	public void txReceived() {
		// do nothing
	}
	
	public static void main(String[] args) throws Exception {
		BT.activateSIP37(true);
		
		new EmulatorWindow(TRTDAO.class);
	}
	
}
