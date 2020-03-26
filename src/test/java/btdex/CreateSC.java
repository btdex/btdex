package btdex;

import bt.BT;
import bt.compiler.Compiler;
import btdex.core.Mediators;
import btdex.sc.SellContract;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;


import java.io.IOException;

public class CreateSC {
    private long feeContract = TestVariables.T_FEE_CONTRACT;

    private BurstID mediator1;
    private BurstID mediator2;

    private BurstValue amount;
    private Class sc;
    private BurstValue security;
    private long accountHash = 0;
    private bt.compiler.Compiler compiled;

    public CreateSC(Class sc, double amount, double security) throws IOException {
        Mediators mediators = new Mediators(true);
        this.mediator1 = mediators.getMediators()[0];
        this.mediator2 = mediators.getMediators()[1];
        this.amount = BurstValue.fromBurst(amount);
        this.security = BurstValue.fromBurst(security);
        this.compiled = BT.compileContract(sc);
        this.sc = sc;
    }


    public String registerSC(String passphrase){
        //Get some BURST for fee(block reward) and write acc to chain
        BT.forgeBlock(passphrase);

        long data[] = { feeContract, mediator1.getSignedLongId(), mediator2.getSignedLongId(), accountHash};

        String name = sc.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(passphrase, compiled.getCode(), compiled.getDataPages(),
                name, name, data, BurstValue.fromPlanck(SellContract.ACTIVATION_FEE),
                BT.getMinRegisteringFee(compiled), 1000).blockingGet();
        BT.forgeBlock(passphrase);
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
}
