package btdex.buyContract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import bt.BT;
import bt.Contract;
import btdex.CreateSC;
import btdex.sc.BuyContract;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.TransactionBroadcast;
import signumj.service.NodeService;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCreateUpdateTakeReopen {
    private static bt.compiler.Compiler compiled;
    private static AT contract;
    private static String makerPass;
    private static long state;
    private static long state_chain;
    private static long taker_chain;
    private static long security;
    private static long amount_chain;
    private static long security_chain;
    private static long balance;
    private static long wantsToBuyInPlanks;
    private static String takerPass;
    private static SignumAddress taker;
    private static NodeService bns = BT.getNode();
    private static CreateSC sc;
    private static long paidFeeForSCstep = BuyContract.LAST_STEP_FEE;

    @Test
    @Order(1)
    public void initSC() throws IOException {
        sc = new CreateSC(BuyContract.class);
        makerPass = Long.toString(System.currentTimeMillis());
        String name = sc.registerSC(makerPass);

        SignumAddress maker = BT.getAddressFromPassphrase(makerPass);
        contract = BT.findContract(maker, name);
        System.out.println("Created contract id " + contract.getId().getID());

        compiled = sc.getCompiled();
    }

    @Test
    @Order(2)
    public void testMediators() {
        long mediator1 = sc.getMediator1();
        long mediator2 = sc.getMediator2();
        long med1_chain = BT.getContractFieldValue(contract, compiled.getField("mediator1").getAddress());
        long med2_chain = BT.getContractFieldValue(contract, compiled.getField("mediator2").getAddress());

        assertEquals(mediator1, med1_chain, "Mediator1 not equal");
        assertEquals(mediator2, med2_chain, "Mediator2 not equal");
    }

    @Test
    @Order(3)
    public void testStateFinished(){
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = BuyContract.STATE_FINISHED;
        assertEquals(state, state_chain, "State not equal");
    }

    private long accBalance(String pass) {
        return (bns.getAccount(BT.getAddressFromPassphrase(pass)).blockingGet()).getBalance().longValue();
    }

    @Test
    @Order(4)
    public void testOfferInit(){

        wantsToBuyInPlanks = Contract.ONE_BURST * 100L; //100 Burst
        security = Contract.ONE_BURST * 10L; //10 Burst
        TransactionBroadcast tb = BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                SignumValue.fromNQT(security + BuyContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000, wantsToBuyInPlanks);
        BT.forgeBlock(tb);
        BT.forgeBlock();
        // should now be open
        state = BuyContract.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        assertEquals(state, state_chain);

        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());
        assertEquals(wantsToBuyInPlanks, amount_chain);
        assertTrue(security < security_chain);

        balance = BT.getContractBalance(contract).longValue();

        assertTrue(balance > security_chain - paidFeeForSCstep);
    }

    @Test
    @Order(5)
    public void initTaker() {
        takerPass = Long.toString(System.currentTimeMillis());
        taker = BT.getAddressFromPassphrase(takerPass);
        //register taker in chain
        BT.forgeBlock(takerPass);
        //fund taker if needed
        while (accBalance(takerPass) < security_chain + BuyContract.ACTIVATION_FEE + wantsToBuyInPlanks) {
            BT.forgeBlock(takerPass);
        }
    }

    @Test
    @Order(6)
    public void testOfferTake() {
        // Take the offer
    	TransactionBroadcast tb = BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                SignumValue.fromNQT(security_chain + BuyContract.ACTIVATION_FEE + wantsToBuyInPlanks), SignumValue.fromSigna(0.1), 100,
                security_chain, amount_chain);
        BT.forgeBlock(tb);
        BT.forgeBlock();

        // order should be taken, waiting for payment
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        state = BuyContract.STATE_WAITING_PAYMT;
        assertEquals(state, state_chain);
        assertEquals(taker.getSignedLongId(), taker_chain);
    }

    @Test
    @Order(7)
    public void testTakerSignal() {
        // Taker signals the payment was received (off-chain)
    	TransactionBroadcast tb = BT.callMethod(takerPass, contract.getId(), compiled.getMethod("reportComplete"),
                SignumValue.fromNQT(BuyContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 100);
        BT.forgeBlock(tb);
        BT.forgeBlock();

        // order should be finished
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        assertEquals(BuyContract.STATE_FINISHED, state_chain);

        balance = BT.getContractBalance(contract).longValue();
        assertTrue(balance == 0, "not enough balance");
        assertTrue(accBalance(makerPass) > amount_chain, "Maker have not received");
    }

    @Test
    @Order(8)
    public void testReopen(){
        wantsToBuyInPlanks = 100_000_000L * 1000L; //1000 Burst
        security = 100_000_000L * 150L; //10 Burst
        TransactionBroadcast tb = BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                SignumValue.fromNQT(security + BuyContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000, wantsToBuyInPlanks);
        BT.forgeBlock(tb);
        BT.forgeBlock();
        // should now be open
        state = BuyContract.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        assertEquals(state, state_chain);

        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());
        assertEquals(wantsToBuyInPlanks, amount_chain);
        assertTrue(security < security_chain);

        balance = BT.getContractBalance(contract).longValue();

        assertTrue(balance > security_chain - paidFeeForSCstep);
    }
}
