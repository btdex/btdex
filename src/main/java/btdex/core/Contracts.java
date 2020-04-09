package btdex.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;

import org.bouncycastle.util.encoders.Hex;

import bt.compiler.Compiler;
import bt.compiler.Method;
import btdex.sc.SellContract;
import btdex.sc.SellNoDepositContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.AT;

public class Contracts {
    private static Compiler contract, contractNoDeposit;
    private static byte[] contractCode;
    private static byte[] contractNoDepositCode;
    
    private static String contractUpdateHash;
    
	private static HashMap<BurstAddress, ContractState> contractsMap = new HashMap<>();
	private static boolean loading = true;
	private static BurstID mostRecentID;
	private static ContractState freeContract, freeNoDepositContract;
	
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

            contractCode = contract.getCode();
            contractNoDepositCode = contractNoDeposit.getCode();
            
            // get the update method hash
        	ByteBuffer b = ByteBuffer.allocate(8);
            b.order(ByteOrder.LITTLE_ENDIAN);
            Method m = contract.getMethod("update");
            b.putLong(m.getHash());
            contractUpdateHash = Hex.toHexString(b.array());
            
            // TODO: remove this condition on the future
            if(Globals.getInstance().isTestnet()) {
            	// start the update thread
            	new UpdateThread().start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Compiler getContract() {
        return contract;
    }

    public static Compiler getContractNoDeposit() {
        return contractNoDeposit;
    }

    public static byte[] getContractCode() {
        return contractCode;
    }

    public static byte[] getContractNoDepositCode() {
        return contractNoDepositCode;
    }
    
    public static String getContractUpdateHash() {
    	return contractUpdateHash;
    }
    
	public static boolean checkContractCode(AT at) {
		byte []code = getContractCode();
		
		if(at.getMachineCode().length < code.length)
			return false;
		
		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i])
				return false;
		}
		return true;
	}
	
	public static boolean checkContractCodeNoDeposit(AT at) {
		byte []code = getContractNoDepositCode();
		
		if(at.getMachineCode().length < code.length)
			return false;
		
		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i])
				return false;
		}
		return true;
	}

	public static Collection<ContractState> getContracts() {
		return contractsMap.values();
	}
	
	private static void updateContracts() {
		// check for new contracts and add them to the list
		mostRecentID = ContractState.addContracts(contractsMap, mostRecentID);
		
		Globals g = Globals.getInstance();
		freeContract = null;
		freeNoDepositContract = null;

		// update the state of every contract for the given market
		for(ContractState s : contractsMap.values()) {
			s.update();
			
			if(s.getType() == ContractState.Type.Standard &&
					s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId() && 
					s.getState() == SellContract.STATE_FINISHED && !s.hasPending())
				freeContract = s;
			if(s.getType() == ContractState.Type.NoDeposit &&
					s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId() &&
					s.getState() == SellNoDepositContract.STATE_FINISHED && !s.hasPending())
				freeNoDepositContract = s;
		}
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
	
	public static ContractState getFreeNoDepositContract() {
		return freeNoDepositContract;
	}
}
