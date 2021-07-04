package btdex;

import java.io.IOException;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import btdex.sc.SellContract;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumValue;

public class CreateSC {
    private long feeContract = BT.getAddressFromPassphrase(BT.PASSPHRASE3).getSignumID().getSignedLongId();

    private long mediator1;
    private long mediator2;

    private String mediatorOnePassword;
    private String mediatorTwoPassword;

    private SignumValue amount;

    private Class<? extends Contract> sc;

    private SignumValue security;

    private long accountHash = 0;
    private bt.compiler.Compiler compiled;

    public CreateSC(Class<? extends Contract> sc) throws IOException {
    	this(sc, 0, 0);
    }
    
    public CreateSC(Class<? extends Contract> sc, double amount, double security) throws IOException {
    	BT.activateCIP20(true);

    	mediatorOnePassword = BT.PASSPHRASE;
        mediatorTwoPassword = BT.PASSPHRASE2;
        mediator1 = SignumCrypto.getInstance().getAddressFromPassphrase(mediatorOnePassword).getSignedLongId();
        mediator2 = SignumCrypto.getInstance().getAddressFromPassphrase(mediatorTwoPassword).getSignedLongId();

        this.amount = SignumValue.fromSigna(amount);
        this.security = SignumValue.fromSigna(security);
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
                name, name, data, SignumValue.fromNQT(SellContract.ACTIVATION_FEE),
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
