package btdex.buyContract.dispute;

import bt.BT;
import btdex.CreateSC;
import btdex.sc.BuyContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.service.BurstNodeService;


public class InitSC {
    private String name;
    private BurstAddress maker;
    private String makerPass;
    private BurstAddress taker;
    private String takerPass;
    private bt.compiler.Compiler compiled;
    private AT contract;

    private BurstAddress mediatorOne;
    private BurstAddress mediatorTwo;

    private String mediatorOnePass;
    private String mediatorTwoPass;

    private long wantsToBuyInPlanks;
    private long feeContract;

    private long security;
    private static BurstNodeService bns = BT.getNode();

    public InitSC() {
        try {
            CreateSC sc = new CreateSC(BuyContract.class);
            makerPass = Long.toString(System.currentTimeMillis());
            name = sc.registerSC(makerPass);
            wantsToBuyInPlanks = 100_000_000L * 1000L; //1000 Burst
            security = 100_000_000L * 200L; //200 Burst
            compiled = sc.getCompiled();
            mediatorOne = BurstAddress.fromId(sc.getMediator1());
            mediatorTwo = BurstAddress.fromId(sc.getMediator2());
            mediatorOnePass = sc.getMediatorOnePassword();
            mediatorTwoPass = sc.getMediatorTwoPassword();
            feeContract = sc.getFeeContract();
            initOffer();
            //init taker
            takerPass = Long.toString(System.currentTimeMillis());
            taker = BT.getBurstAddressFromPassphrase(takerPass);
            //register taker in chain
            BT.forgeBlock(takerPass);
            takeOffer();

        } catch (Exception e) {
            System.out.println("Something went wrong. " + e);
        }
    }
    private long accBalance(String pass) {
        return (bns.getAccount(BT.getBurstAddressFromPassphrase(pass), null, null).blockingGet()).getBalance().longValue();
    }

    public void initOffer(){
        //fund maker if needed
        while(accBalance(makerPass) < security + BuyContract.ACTIVATION_FEE){
            BT.forgeBlock(makerPass);
        }
        maker = BT.getBurstAddressFromPassphrase(makerPass);
        contract = BT.findContract(maker, name);
        //init offer
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(security + BuyContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1),
                1000, wantsToBuyInPlanks).blockingGet();
        BT.forgeBlock();
    }

    public void takeOffer() {
        //fund taker if needed
        while (accBalance(takerPass) < security + BuyContract.ACTIVATION_FEE + wantsToBuyInPlanks) {
            BT.forgeBlock(takerPass);
        }
        // Take the offer
        long amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        long security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(security_chain + BuyContract.ACTIVATION_FEE + wantsToBuyInPlanks), BurstValue.fromBurst(0.1), 100,
                security_chain, amount_chain).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
    }

    public long getContractFieldValue(String field) {
        AT contract = BT.findContract(maker, name);
        if(contract == null) return -1;

        int addr = compiled.getFieldAddress(field);
        if(addr == -1) return -2;

        return BT.getContractFieldValue(contract, addr);
    }

    public void dispute(BurstAddress who, long amountToMaker, long amountToTaker) {
        BT.callMethod(who == maker ? makerPass : who == taker ? takerPass : who == mediatorOne ? mediatorOnePass : mediatorTwoPass,
                contract.getId(), compiled.getMethod("dispute"),
                BurstValue.fromPlanck(BuyContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                amountToMaker, amountToTaker).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
    }

    public void complete(BurstAddress who) {
        BT.callMethod(who == maker? makerPass : takerPass, contract.getId(), compiled.getMethod("reportComplete"),
                BurstValue.fromPlanck(BuyContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100).blockingGet();
    }

    public void withdraw() {
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(BuyContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000,
                0).blockingGet();
    }

    public BurstAddress getMaker() {
        return maker;
    }

    public BurstAddress getTaker() {
        return taker;
    }

    public long getSCbalance() {
        return BT.findContract(maker, name).getBalance().longValue();
    }

    public long getMakerBalance() {
        return bns.getAccount(maker, null, null).blockingGet().getBalance().longValue();
    }

    public long getTakerBalance() {
        return bns.getAccount(taker, null, null).blockingGet().getBalance().longValue();
    }

    public long getFeeContractBalance() {
        return bns.getAccount(BurstAddress.fromId(feeContract), null, null).blockingGet().getBalance().longValue();
    }

    public BurstAddress getMediatorOne() {
        return mediatorOne;
    }

    public BurstAddress getMediatorTwo() {
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
