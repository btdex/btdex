package btdex.core;

import bt.compiler.Compiler;
import btdex.sc.SellContract;
import btdex.sc.SellNoDepositContract;

import java.io.IOException;

public class Contracts {
    private static Compiler contract, contractNoDeposit;
    private static byte[] contractCode;
    private static byte[] contractNoDepositCode;

    public static void addContracts() {
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
}
