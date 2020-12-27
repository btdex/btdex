package btdex.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import bt.compiler.Compiler;
import btdex.sc.BuyContract;
import btdex.sc.BuyContract2;
import btdex.sc.SellContract;
import btdex.sc.SellContract2;
import btdex.sc.SellNoDepositContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.Block;
import burst.kit.entity.response.Transaction;

public class Contracts {
    private static Compiler compilerSell[], compilerNoDeposit, compilerBuy[];

    private static String contractTakeHash[], contractBuyTakeHash[];

	private static HashMap<BurstAddress, ContractState> contractsMap = new HashMap<>();
	private static boolean loading = true;
	private static BurstID mostRecentID;
	private static BurstID lastBlock;
	private static ContractState freeContract, freeNoDepositContract, freeBuyContract;
	private static boolean registering;
	
	private static ArrayList<ContractTrade> trades = new ArrayList<>();

	private static Logger logger = LogManager.getLogger();

	static class UpdateThread extends Thread {
		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()) {
				updateContracts();
				loading = false;
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					logger.error("InterruptedException {}", e.getLocalizedMessage());
					break;
				}
			}
		}
	}

	public static boolean isLoading() {
		return loading;
	}

    static {
        try {
            compilerSell = new Compiler[2];
            contractTakeHash = new String[2];
            compilerSell[0] = new Compiler(SellContract2.class);
            compilerSell[1] = new Compiler(SellContract.class);
            int i = 0;
            for(Compiler c : compilerSell) {
            	c.compile();
            	c.link();
            	
                // get the update method hash
            	ByteBuffer b = ByteBuffer.allocate(8);
                b.order(ByteOrder.LITTLE_ENDIAN);
                b.putLong(c.getMethod("take").getHash());
                contractTakeHash[i++] = Hex.toHexString(b.array());
            }

            compilerNoDeposit = new Compiler(SellNoDepositContract.class);
            compilerNoDeposit.compile();
            compilerNoDeposit.link();

            compilerBuy = new Compiler[2];
            contractBuyTakeHash = new String[2];
            compilerBuy[0] = new Compiler(BuyContract2.class);
            compilerBuy[1] = new Compiler(BuyContract.class);
            i = 0;
            for(Compiler c : compilerBuy) {
            	c.compile();
            	c.link();
            	
            	// get the update method hash
            	ByteBuffer b = ByteBuffer.allocate(8);
                b.order(ByteOrder.LITTLE_ENDIAN);
                b.putLong(c.getMethod("take").getHash());
                contractBuyTakeHash[i++] = Hex.toHexString(b.array());
            }

           	// start the update thread
           	new UpdateThread().start();
        } catch (IOException e) {
        	logger.error("IOException: {}", e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public static Compiler getCompiler(ContractType type) {
    	switch (type) {
		case BUY:
			return compilerBuy[0];
		case BUY_OLD:
			return compilerBuy[1];
		case SELL:
			return compilerSell[0];
		case SELL_OLD:
			return compilerSell[1];
		case NO_DEPOSIT:
			return compilerNoDeposit;
		default:
		}
        return null;
    }
    
    public static ArrayList<ContractTrade> getTrades(){
    	return trades;
    }

    public static Compiler getContractNoDeposit() {
        return compilerNoDeposit;
    }

    public static Compiler getContractBuy() {
        return compilerBuy[0];
    }

    public static byte[] getCodeSell() {
        return compilerSell[0].getCode();
    }

    public static byte[] getCodeNoDeposit() {
        return compilerNoDeposit.getCode();
    }

    public static byte[] getCodeBuy() {
        return compilerBuy[0].getCode();
    }

    public static String getContractTakeHash(ContractType type) {
    	if(type == ContractType.BUY)
    		return contractBuyTakeHash[0];
    	return contractTakeHash[0];
    }
    
    public static ContractType getContractType(AT at) {
   		if(checkContractCode(at, compilerSell[0].getCode()))
    		return ContractType.SELL;
   		if(checkContractCode(at, compilerSell[1].getCode()))
    		return ContractType.SELL_OLD;
   		if(checkContractCode(at, compilerBuy[0].getCode()))
    		return ContractType.BUY;
   		if(checkContractCode(at, compilerBuy[1].getCode()))
    		return ContractType.BUY_OLD;
    	if (Contracts.checkContractCode(at, Contracts.getCodeNoDeposit()))
    		return ContractType.NO_DEPOSIT;
    	return ContractType.INVALID;
    }

	private static boolean checkContractCode(AT at, byte []code) {
		if(at.getMachineCode().length < code.length) {
			logger.trace("AT code {} less then {} for {}", at.getMachineCode().length, code.length, at.getId());
			return false;
		}

		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i]){
				logger.trace("AT code do not match for {}", at.getId());
				return false;
			}
		}

		// TODO, also check the creation transaction and the initial data
		return true;
	}

	public static Collection<ContractState> getContracts() {
		return contractsMap.values();
	}

	private static void updateContracts() {
		try {
			// check for new contracts and add them to the list
			mostRecentID = ContractState.addContracts(contractsMap, mostRecentID);

			Globals g = Globals.getInstance();

			// check if we have a new block or not
			Block[] latestBlocks = g.getNS().getBlocks(0, 1).blockingGet();
			boolean noNewBlock = latestBlocks[0].getId().equals(lastBlock);

			ContractState updatedFreeContract = null;
			ContractState updatedBuyFreeContract = null;
			ContractState updatedFreeNoDepositContract = null;

			// check for the pending transactions
			Transaction[] utxs = g.getNS().getUnconfirmedTransactions(g.getAddress()).blockingGet();
			
			boolean checkRegistering = false;
			for (Transaction tx : utxs) {
				if(tx.getSender().equals(g.getAddress()) && tx.getType() == 22 && tx.getSubtype() == 0) {
					checkRegistering = true;
					break;
				}
			}
			registering = checkRegistering;

			// build the trade history
			ArrayList<ContractTrade> tradeHistory = new ArrayList<>();

			// update the state of every contract
			for(ContractState s : contractsMap.values()) {
				s.update(utxs, noNewBlock);

				if((s.getType() == ContractType.SELL && s.getVersion() == 2) &&
						s.getCreator().equals(g.getAddress()) &&
						s.getState() == SellContract.STATE_FINISHED && !s.hasPending() &&
						g.getMediators().areMediatorsAccepted(s) &&
						s.getATVersion()>1)
					updatedFreeContract = s;
				else if((s.getType() == ContractType.BUY && s.getVersion() == 2) &&
						s.getCreator().equals(g.getAddress()) &&
						s.getState() == SellContract.STATE_FINISHED && !s.hasPending() &&
						g.getMediators().areMediatorsAccepted(s) &&
						s.getATVersion()>1)
					updatedBuyFreeContract = s;
				else if(s.getType() == ContractType.NO_DEPOSIT &&
						s.getCreator().equals(g.getAddress()) &&
						s.getState() == SellNoDepositContract.STATE_FINISHED && !s.hasPending() &&
						g.getMediators().areMediatorsAccepted(s) &&
						s.getATVersion()>1)
					updatedFreeNoDepositContract = s;

				tradeHistory.addAll(s.getTrades());
			}
			lastBlock = latestBlocks[0].getId();
			
			// now we sort the trades on time
			tradeHistory.sort(new Comparator<ContractTrade>() {
				@Override
				public int compare(ContractTrade t1, ContractTrade t2) {
					return t2.getTimestamp().getAsDate().compareTo(t1.getTimestamp().getAsDate());
				}
			});

			// the list with all trades
			trades = tradeHistory;

			// TODO: maybe a lock around this
			freeContract = updatedFreeContract;
			freeBuyContract = updatedBuyFreeContract;
			freeNoDepositContract = updatedFreeNoDepositContract;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception", e.getLocalizedMessage());
		}
	}

	public static long[] getNewContractData() {
		Mediators mediators = Globals.getInstance().getMediators();
		BurstID[] med = mediators.getTwoRandomMediators();

		long data[] = new long[3];
		data[0] = Globals.getInstance().getFeeContract();

		data[1] = med[0].getSignedLongId();
		data[2] = med[1].getSignedLongId();

		return data;
	}
	
	public static boolean isRegistering() {
		return registering;
	}

	public static ContractState getFreeContract() {
		return freeContract;
	}

	public static ContractState getFreeBuyContract() {
		return freeBuyContract;
	}

	public static ContractState getFreeNoDepositContract() {
		return freeNoDepositContract;
	}
}
