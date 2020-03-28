package btdex.dispute;

import bt.BT;
import btdex.CreateSC;
import btdex.core.Mediators;
import btdex.sc.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.service.BurstNodeService;


public class InitSC{
    private String name;
    private BurstAddress maker;
    private String makerPass;
    private BurstAddress taker;
    private String takerPass;
    private bt.compiler.Compiler compiled;
    private AT contract;

    private BurstAddress mediatorOne;

    private BurstAddress mediatorTwo;
    private String mediatorOnePass = BT.PASSPHRASE;
    private String mediatorTwoPass = BT.PASSPHRASE2;
    private long amount;

    private long security;
    private long sent;
    private static BurstNodeService bns = BT.getNode();

    public InitSC() {
        try {
            CreateSC sc = new CreateSC(SellContract.class, 10000, 100);
            makerPass = Long.toString(System.currentTimeMillis());
            name = sc.registerSC(makerPass);
            amount = sc.getAmount();
            security = sc.getSecurity();
            sent = amount + security + SellContract.ACTIVATION_FEE;
            compiled = sc.getCompiled();
            Mediators md = new Mediators(true);
            mediatorOne = BurstAddress.fromId(md.getMediators()[0]);
            mediatorTwo = BurstAddress.fromId(md.getMediators()[1]);
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
        return (bns.getAccount(BT.getBurstAddressFromPassphrase(pass)).blockingGet()).getBalance().longValue();
    }
    public void initOffer(){
        //fund maker if needed
        while(accBalance(makerPass) < sent){
            BT.forgeBlock(makerPass);
        }
        maker = BT.getBurstAddressFromPassphrase(makerPass);
        contract = BT.findContract(maker, name);
        //init offer
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(sent), BurstValue.fromBurst(0.1), 1000,
                security).blockingGet();
        BT.forgeBlock();
    }

    public void takeOffer() {
        //fund taker if needed
        while (accBalance(takerPass) < security + SellContract.ACTIVATION_FEE) {
            BT.forgeBlock(takerPass);
        }
        // Take the offer
        long amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                security, amount_chain).blockingGet();
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
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                amountToMaker, amountToTaker).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
    }

    public void complete(BurstAddress who) {
        BT.callMethod(who == maker? makerPass : takerPass, contract.getId(), compiled.getMethod("reportComplete"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100).blockingGet();
    }

    public void withdraw() {
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000,
                0).blockingGet();
    }

    public long getAmount() {
        return amount;
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
        return bns.getAccount(maker).blockingGet().getBalance().longValue();
    }

    public long getTakerBalance() {
        return bns.getAccount(taker).blockingGet().getBalance().longValue();
    }

    public BurstAddress getMediatorOne() {
        return mediatorOne;
    }

    public BurstAddress getMediatorTwo() {
        return mediatorTwo;
    }

}
