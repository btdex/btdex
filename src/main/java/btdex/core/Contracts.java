package btdex.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;

import org.bouncycastle.util.encoders.Hex;

import bt.compiler.Compiler;
import btdex.sc.BuyContract;
import btdex.sc.SellContract;
import btdex.sc.SellNoDepositContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.Transaction;

public class Contracts {
    private static Compiler contract, contractNoDeposit, contractBuy;
    private static byte[] contractCode;
    private static byte[] contractNoDepositCode;
    private static byte[] contractBuyCode;
    
    private static String contractTakeHash, contractBuyTakeHash;
    
	private static HashMap<BurstAddress, ContractState> contractsMap = new HashMap<>();
	private static boolean loading = true;
	private static BurstID mostRecentID;
	private static ContractState freeContract, freeNoDepositContract, freeBuyContract;
	
	static class UpdateThread extends Thread {
		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()) {
				updateContracts();
				loading = false;
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
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
            contract = new Compiler(SellContract.class);
            contract.compile();
            contract.link();

            contractNoDeposit = new Compiler(SellNoDepositContract.class);
            contractNoDeposit.compile();
            contractNoDeposit.link();
            
            contractBuy = new Compiler(BuyContract.class);
            contractBuy.compile();
            contractBuy.link();

            contractCode = contract.getCode();
            contractNoDepositCode = contractNoDeposit.getCode();
            contractBuyCode = contractBuy.getCode();
            
            // get the update method hash
        	ByteBuffer b = ByteBuffer.allocate(8);
            b.order(ByteOrder.LITTLE_ENDIAN);
            b.putLong(contract.getMethod("take").getHash());
            contractTakeHash = Hex.toHexString(b.array());

            b.clear();
            b.putLong(contractBuy.getMethod("take").getHash());
            contractBuyTakeHash = Hex.toHexString(b.array());
            
            // TODO: remove this condition on the future
            if(Globals.getInstance().isTestnet()) {
            	// start the update thread
            	new UpdateThread().start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Compiler getContract(ContractState.Type type) {
    	switch (type) {
		case Buy:
			return contractBuy;
		case NoDeposit:
			return contractNoDeposit;
		default:
		}
        return contract;
    }

    public static Compiler getContractNoDeposit() {
        return contractNoDeposit;
    }
    
    public static Compiler getContractBuy() {
        return contractBuy;
    }

    public static byte[] getContractCode() {
        return contractCode;
    }

    public static byte[] getContractNoDepositCode() {
        return contractNoDepositCode;
    }
    
    public static byte[] getContractBuyCode() {
        return contractBuyCode;
    }
    
    public static String getContractTakeHash(ContractState.Type type) {
    	if(type == ContractState.Type.Buy)
    		return contractBuyTakeHash;
    	return contractTakeHash;
    }
    
	public static boolean checkContractCode(AT at, byte []code) {
		if(at.getMachineCode().length < code.length)
			return false;
		
		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i])
				return false;
		}
		
		// TODO, also check the creation transaction and the initial data
		return true;
	}
    
	public static Collection<ContractState> getContracts() {
		return contractsMap.values();
	}
	
	private static void updateContracts() {
		// check for new contracts and add them to the list
		mostRecentID = ContractState.addContracts(contractsMap, mostRecentID);
		
		Globals g = Globals.getInstance();
		ContractState updatedFreeContract = null;
		ContractState updatedBuyFreeContract = null;
		ContractState updatedFreeNoDepositContract = null;
		
		// check for the pending transactions
		Transaction[] utxs = g.getNS().getUnconfirmedTransactions(g.getAddress()).blockingGet();

		// update the state of every contract
		for(ContractState s : contractsMap.values()) {
			s.update(utxs);
			
			if(s.getType() == ContractState.Type.Standard &&
					s.getCreator().equals(g.getAddress()) && 
					s.getState() == SellContract.STATE_FINISHED && !s.hasPending())
				updatedFreeContract = s;
			else if(s.getType() == ContractState.Type.Buy &&
					s.getCreator().equals(g.getAddress()) && 
					s.getState() == SellContract.STATE_FINISHED && !s.hasPending())
				updatedBuyFreeContract = s;
			else if(s.getType() == ContractState.Type.NoDeposit &&
					s.getCreator().equals(g.getAddress()) &&
					s.getState() == SellNoDepositContract.STATE_FINISHED && !s.hasPending())
				updatedFreeNoDepositContract = s;
		}
		
		// TODO: maybe a lock around this
		freeContract = updatedFreeContract;
		freeBuyContract = updatedBuyFreeContract;
		freeNoDepositContract = updatedFreeNoDepositContract;
	}

	public static long[] getNewContractData(Boolean testnet) {
		Mediators mediators = new Mediators(testnet);
		BurstID[] med = mediators.getTwoRandomMediators();

		long data[] = new long[3];
		data[0] = Constants.FEE_CONTRACT;

		data[1] = med[0].getSignedLongId();
		data[2] = med[1].getSignedLongId();

		return data;
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
