package btdex;

import java.io.IOException;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import btdex.sc.SellContract;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstValue;

public class CreateSC {
    private long feeContract = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE3).getBurstID().getSignedLongId();

    private long mediator1;
    private long mediator2;

    private String mediatorOnePassword;
    private String mediatorTwoPassword;

    private BurstValue amount;

    private Class<? extends Contract> sc;

    private BurstValue security;

    private long accountHash = 0;
    private bt.compiler.Compiler compiled;

    public CreateSC(Class<? extends Contract> sc) throws IOException {
    	this(sc, 0, 0);
    }
    
    public CreateSC(Class<? extends Contract> sc, double amount, double security) throws IOException {
    	BT.activateCIP20(true);

    	mediatorOnePassword = BT.PASSPHRASE;
        mediatorTwoPassword = BT.PASSPHRASE2;
        mediator1 = BurstCrypto.getInstance().getBurstAddressFromPassphrase(mediatorOnePassword).getSignedLongId();
        mediator2 = BurstCrypto.getInstance().getBurstAddressFromPassphrase(mediatorTwoPassword).getSignedLongId();

        this.amount = BurstValue.fromBurst(amount);
        this.security = BurstValue.fromBurst(security);
        this.compiled = BT.compileContract(sc);
        this.sc = sc;
    }

    public String registerSC(String passphrase){
        //Get some BURST for fee(block reward) and write acc to chain
        BT.forgeBlock(passphrase);
        String name = register(passphrase);
        BT.forgeBlock(passphrase);
        return name;
    }

    private String register(String passphrase) {
        long data[] = { feeContract, mediator1, mediator2, accountHash};

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

    public long getMediator1() {
        return mediator1;
    }

    public long getMediator2() {
        return mediator2;
    }

    public String getMediatorOnePassword() {
        return mediatorOnePassword;
    }

    public String getMediatorTwoPassword() {
        return mediatorTwoPassword;
    }
}
