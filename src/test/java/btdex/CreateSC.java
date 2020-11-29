package btdex;

import java.io.IOException;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import btdex.core.Mediators;
import btdex.sc.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;

public class CreateSC {
    private long feeContract = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE3).getBurstID().getSignedLongId();

    private BurstID mediator1;
    private BurstID mediator2;

    private String mediatorOnePassword;
    private String mediatorTwoPassword;

    private BurstValue amount;

    private Class<? extends Contract> sc;

    private BurstValue security;

    private long accountHash = 0;
    private bt.compiler.Compiler compiled;
    public CreateSC(Class<? extends Contract> sc, double amount, double security) throws IOException {
        Mediators mediators = new Mediators(true);
        BurstID[] mediatorsID = mediators.getTwoRandomMediators();
        mediator1 = mediatorsID[0];
        mediator2 = mediatorsID[1];
        mediatorOnePassword = getMediatorPassword(mediator1);
        mediatorTwoPassword = getMediatorPassword(mediator2);

        this.amount = BurstValue.fromBurst(amount);
        this.security = BurstValue.fromBurst(security);
        this.compiled = BT.compileContract(sc);
        this.sc = sc;
    }

    public CreateSC(Class<? extends Contract> sc) throws IOException {
        Mediators mediators = new Mediators(true);
        BurstID[] mediatorsID = mediators.getTwoRandomMediators();
        mediator1 = mediatorsID[0];
        mediator2 = mediatorsID[1];
        mediatorOnePassword = getMediatorPassword(mediator1);
        mediatorTwoPassword = getMediatorPassword(mediator2);

        this.compiled = BT.compileContract(sc);
        this.sc = sc;
    }

    private String getMediatorPassword(BurstID mediatorID) {
        BurstAddress mediator = BurstAddress.fromId(mediatorID);

        if(mediator.equals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE)))
            return BT.PASSPHRASE;
        else if(mediator.equals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2)))
            return BT.PASSPHRASE2;
        else if(mediator.equals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE4)))
            return BT.PASSPHRASE4;
        else if(mediator.equals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE5)))
            return BT.PASSPHRASE5;
        else return null;
    }

    public String registerSC(String passphrase){
        //Get some BURST for fee(block reward) and write acc to chain
        BT.forgeBlock(passphrase);
        String name = register(passphrase);
        BT.forgeBlock(passphrase);
        return name;
    }

    private String register(String passphrase) {
        long data[] = { feeContract, mediator1.getSignedLongId(), mediator2.getSignedLongId(), accountHash};

        String name = sc.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(passphrase, compiled.getCode(), compiled.getDataPages(),
                name, name, data, BurstValue.fromPlanck(SellContract.ACTIVATION_FEE),
                BT.getMinRegisteringFee(compiled), 1000).blockingGet();
        return name;
    }

    public long getAmount() {
        return amount.longValue();
    }

    public long getSecurity() {
        return security.longValue();
    }

    public Compiler getCompiled() {
        return compiled;
    }

    public long getFeeContract() {
        return feeContract;
    }

    public BurstID getMediator1() {
        return mediator1;
    }

    public BurstID getMediator2() {
        return mediator2;
    }

    public String getMediatorOnePassword() {
        return mediatorOnePassword;
    }

    public String getMediatorTwoPassword() {
        return mediatorTwoPassword;
    }
}
