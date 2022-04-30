package btdex.sellContract.dispute;

import bt.BT;
import btdex.CreateSC;
import btdex.sc.SellContract;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.TransactionBroadcast;
import signumj.service.NodeService;


public class InitSC{
    private String name;
    private SignumAddress maker;
    private String makerPass;
    private SignumAddress taker;
    private String takerPass;
    private bt.compiler.Compiler compiled;
    private AT contract;

    private SignumAddress mediatorOne;
    private SignumAddress mediatorTwo;

    private String mediatorOnePass;
    private String mediatorTwoPass;

    private long amount;
    private long feeContract;

    private long security;
    private long sent;
    private static NodeService bns = BT.getNode();

    public InitSC() {
        try {
            CreateSC sc = new CreateSC(SellContract.class, 10000, 100);
            makerPass = Long.toString(System.currentTimeMillis());
            name = sc.registerSC(makerPass);
            amount = sc.getAmount();
            security = sc.getSecurity();
            sent = amount + security + SellContract.ACTIVATION_FEE;
            compiled = sc.getCompiled();
            mediatorOnePass = BT.PASSPHRASE;
            mediatorTwoPass = BT.PASSPHRASE2;
            mediatorOne = SignumCrypto.getInstance().getAddressFromPassphrase(mediatorOnePass);
            mediatorOne = SignumCrypto.getInstance().getAddressFromPassphrase(mediatorTwoPass);
            feeContract = sc.getFeeContract();
            initOffer();
            //init taker
            takerPass = Long.toString(System.currentTimeMillis());
            taker = BT.getAddressFromPassphrase(takerPass);
            //register taker in chain
            BT.forgeBlock(takerPass);
            takeOffer();

        } catch (Exception e) {
            System.out.println("Something went wrong. " + e);
        }
    }
    private long accBalance(String pass) {
        return (bns.getAccount(BT.getAddressFromPassphrase(pass)).blockingGet()).getBalance().longValue();
    }

    public void initOffer(){
        //fund maker if needed
        while(accBalance(makerPass) < sent){
            BT.forgeBlock(makerPass);
        }
        maker = BT.getAddressFromPassphrase(makerPass);
        contract = BT.findContract(maker, name);
        //init offer
        TransactionBroadcast tb = BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                SignumValue.fromNQT(sent), SignumValue.fromSigna(0.1), 1000,
                security);
        BT.forgeBlock(tb);
    }

    public void takeOffer() {
        //fund taker if needed
        while (accBalance(takerPass) < security + SellContract.ACTIVATION_FEE) {
            BT.forgeBlock(takerPass);
        }
        // Take the offer
        long amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        TransactionBroadcast tb = BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                SignumValue.fromNQT(security + SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 100,
                security, amount_chain);
        BT.forgeBlock(tb);
        BT.forgeBlock();
    }

    public long getContractFieldValue(String field) {
        AT contract = BT.findContract(maker, name);
        if(contract == null) return -1;

        int addr = compiled.getFieldAddress(field);
        if(addr == -1) return -2;

        return BT.getContractFieldValue(contract, addr);
    }

    public void dispute(SignumAddress who, long amountToMaker, long amountToTaker) {
    	TransactionBroadcast tb = BT.callMethod(who == maker ? makerPass : who == taker ? takerPass : who == mediatorOne ? mediatorOnePass : mediatorTwoPass,
                contract.getId(), compiled.getMethod("dispute"),
                SignumValue.fromNQT(SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 100,
                amountToMaker, amountToTaker);
        BT.forgeBlock(tb);
        BT.forgeBlock();
    }

    public void complete(SignumAddress who) {
        BT.callMethod(who == maker? makerPass : takerPass, contract.getId(), compiled.getMethod("reportComplete"),
                SignumValue.fromNQT(SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 100);
    }

    public void withdraw() {
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                SignumValue.fromNQT(SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000,
                0);
    }

    public long getAmount() {
        return amount;
    }

    public SignumAddress getMaker() {
        return maker;
    }

    public SignumAddress getTaker() {
        return taker;
    }

    public long getSCbalance() {
        return BT.findContract(maker, name).getBalance().longValue();
    }

    public long getMakerBalance() {
        return bns.getAccount(maker).blockingGet().getBalance().longValue();
    }

    public long getTakerBalance() {
        return bns.getAccount(taker).blockingGet().getBalance().longValue();
    }

    public long getFeeContractBalance() {
        return bns.getAccount(SignumAddress.fromId(feeContract)).blockingGet().getBalance().longValue();
    }

    public SignumAddress getMediatorOne() {
        return mediatorOne;
    }

    public SignumAddress getMediatorTwo() {
        return mediatorTwo;
    }

    public String getMediatorOnePass() {
        return mediatorOnePass;
    }

    public String getMediatorTwoPass() {
        return mediatorTwoPass;
    }

    public long getFeeContract() {
        return feeContract;
    }

}
