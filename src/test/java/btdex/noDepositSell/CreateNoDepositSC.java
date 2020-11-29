package btdex.noDepositSell;

import java.io.IOException;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import btdex.core.Mediators;
import btdex.sc.SellNoDepositContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.service.BurstNodeService;

public class CreateNoDepositSC {
    private long feeContract = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE3).getBurstID().getSignedLongId();

    private BurstID mediator1;
    private BurstID mediator2;

    private String mediatorOnePassword;
    private String mediatorTwoPassword;

    private long lockMinutes;

    private String name;

    private BurstAddress maker;

    private Class<? extends Contract> sc = SellNoDepositContract.class;
    private bt.compiler.Compiler compiled;

    private BurstNodeService bns = BT.getNode();

    public CreateNoDepositSC(long lockMinutes) throws IOException{
        Mediators mediators = new Mediators(true);
        BurstID[] mediatorsID = mediators.getTwoRandomMediators();
        mediator1 = mediatorsID[0];
        mediator2 = mediatorsID[1];
        mediatorOnePassword = getMediatorPassword(mediator1);
        mediatorTwoPassword = getMediatorPassword(mediator2);

        this.lockMinutes = lockMinutes;
        compiled = BT.compileContract(sc);
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
    public void registerSC(String passphrase){
        //Get some BURST for fee(block reward) and write acc to chain
        BT.forgeBlock(passphrase);
        register(passphrase);
        BT.forgeBlock(passphrase);
    }

    private void register(String passphrase) {
        long data[] = { feeContract, mediator1.getSignedLongId(), mediator2.getSignedLongId(), lockMinutes};
        String name = sc.getSimpleName() + System.currentTimeMillis() % 200;
        BT.registerContract(passphrase, compiled.getCode(), compiled.getDataPages(),
                name, name, data, BurstValue.fromPlanck(SellNoDepositContract.ACTIVATION_FEE),
                BT.getMinRegisteringFee(compiled), 1000).blockingGet();

        this.name = name;
        maker = BT.getBurstAddressFromPassphrase(passphrase);
    }


    public long getContractFieldValue(String field) {
        AT contract = BT.findContract(maker, name);
        if(contract == null) return -1;

        int addr = compiled.getFieldAddress(field);
        if(addr == -1) return -2;

        return BT.getContractFieldValue(contract, addr);
    }

    public String getName() {
        return name;
    }

    public long getFeeContractBalance() {
        return bns.getAccount(BurstAddress.fromId(feeContract)).blockingGet().getBalance().longValue();
    }

    public Compiler getCompiled() {
        return compiled;
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
