package btdex.core;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import bt.compiler.Compiler;
import btdex.sc.SellContract;
import btdex.sc.SellNoDepositContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.AT;

public class Contracts {
    private static Compiler contract, contractNoDeposit;
    private static byte[] contractCode;
    private static byte[] contractNoDepositCode;
    
	private static HashMap<BurstAddress, ContractState> contractsMap = new HashMap<>();
	private static BurstID mostRecentID;
	private static ContractState freeContract, freeNoDepositContract;

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
    
	static boolean checkContractCode(AT at) {
		byte []code = getContractCode();
		
		if(at.getMachineCode().length < code.length)
			return false;
		
		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i])
				return false;
		}
		return true;
	}
	
	static boolean checkContractCodeNoDeposit(AT at) {
		byte []code = getContractNoDepositCode();
		
		if(at.getMachineCode().length < code.length)
			return false;
		
		for (int i = 0; i < code.length; i++) {
			if(at.getMachineCode()[i] != code[i])
				return false;
		}
		return true;
	}
    
	public static Collection<ContractState> updateContracts() {
		// check for new contracts and add them to the list
		mostRecentID = ContractState.addContracts(contractsMap, mostRecentID);
		
		Globals g = Globals.getInstance();

		// update the state of every contract for the given market
		for(ContractState s : contractsMap.values()) {
			s.update();
			
			if(s.getType() == ContractState.Type.Standard &&
					s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId() && 
					s.getState() == SellContract.STATE_FINISHED)
				freeContract = s;
			if(s.getType() == ContractState.Type.NoDeposit &&
					s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId() &&
					s.getState() == SellNoDepositContract.STATE_FINISHED)
				freeNoDepositContract = s;
		}
		return contractsMap.values();
	}
	
	public static ContractState getFreeContract() {
		return freeContract;
	}
	
	public static ContractState getFreeNoDepositContract() {
		return freeNoDepositContract;
	}
}
