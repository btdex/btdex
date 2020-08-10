package btdex.sellContract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import bt.BT;
import btdex.CreateSC;
import btdex.sc.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.service.BurstNodeService;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestInvalidTakeReopenWithdraw extends BT {
    private static bt.compiler.Compiler compiled;
    private static AT contract;
    private static String makerPass;
    private static long state;
    private static long state_chain;
    private static long taker_chain;
    private static long amount;
    private static long security;
    private static long sent;
    private static long amount_chain;
    private static long security_chain;
    private static long balance;
    private static String takerPass;
    private static BurstAddress taker;
    private static BurstNodeService bns = BT.getNode();
    private static CreateSC sc;

    @Test
    @Order(1)
    public void initSC() throws IOException {
        sc = new CreateSC(SellContract.class, 10000, 100);
        makerPass = Long.toString(System.currentTimeMillis());
        String name = sc.registerSC(makerPass);

        BurstAddress maker = BT.getBurstAddressFromPassphrase(makerPass);
        contract = BT.findContract(maker, name);
        System.out.println("Created contract id " + contract.getId().getID());

        compiled = sc.getCompiled();
        amount = sc.getAmount();
        security = sc.getSecurity();
        sent = amount + security + SellContract.ACTIVATION_FEE;
    }

    @Test
    @Order(2)
    public void testMediators() {
        BurstID mediator1 = sc.getMediator1();
        BurstID mediator2 = sc.getMediator2();
        long med1_chain = BT.getContractFieldValue(contract, compiled.getField("mediator1").getAddress());
        long med2_chain = BT.getContractFieldValue(contract, compiled.getField("mediator2").getAddress());

        assertEquals(mediator1.getSignedLongId(), med1_chain, "Mediator1 not equal");
        assertEquals(mediator2.getSignedLongId(), med2_chain, "Mediator2 not equal");
    }

    @Test
    @Order(3)
    public void testStateFinished(){
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellContract.STATE_FINISHED;
        assertEquals(state, state_chain, "State not equal");
    }

    private long accBalance(String pass) {
        return (bns.getAccount(BT.getBurstAddressFromPassphrase(pass)).blockingGet()).getBalance().longValue();
    }

    @Test
    @Order(4)
    public void testOfferInit(){
        //fund maker if needed
        while(accBalance(makerPass) < sent){
            BT.forgeBlock(makerPass);
        }
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(sent), BurstValue.fromBurst(0.1), 1000,
                security).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // should now be open
        state = SellContract.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(state, state_chain);
        // assertEquals(pauseTimeout, pauseTimeout_chain);
        assertTrue(amount_chain > amount);
        assertEquals(security, security_chain);

        balance = BT.getContractBalance(contract).longValue();
        System.out.println("balance " + balance );
        assertTrue(balance > amount + security, "not enough balance");
    }

    @Test
    @Order(5)
    public void initTaker() {
        takerPass = Long.toString(System.currentTimeMillis());
        taker = BT.getBurstAddressFromPassphrase(takerPass);
        //register taker in chain
        BT.forgeBlock(takerPass);
        //fund taker if needed
        while (accBalance(takerPass) < security + SellContract.ACTIVATION_FEE) {
            BT.forgeBlock(takerPass);
        }
    }

    @Test
    @Order(7)
    public void testInvalidTake() {
        // Invalid take, not enough security
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE*2), BurstValue.fromBurst(0.1), 100,
                security, amount_chain).blockingGet();
        balance = BT.getContractBalance(contract).longValue();
        System.out.println("Contract fees for failed take: " + BurstValue.fromPlanck(sent-balance-SellContract.ACTIVATION_FEE));

        // order should not be taken
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        long taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        assertEquals(SellContract.STATE_OPEN, state_chain);
        assertEquals(0, taker_chain);
        BT.forgeBlock();
    }

    @Test
    @Order(8)
    public void testOfferTake() {
        // Take the offer
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                security, amount_chain).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        System.out.println("balance " + BT.getContractBalance(contract).longValue());

        // order should be taken, waiting for payment
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        state = SellContract.STATE_WAITING_PAYMT;
        assertEquals(state, state_chain);
        assertEquals(taker.getSignedLongId(), taker_chain);
    }

    @Test
    @Order(9)
    public void testContractsBalanceAfterOfferTake() {
        balance = BT.getContractBalance(contract).longValue();
        assertTrue(balance > amount + security * 2, "not enough balance");
        System.out.println("Contract fees to take: " + BurstValue.fromPlanck(sent - balance));
    }

    @Test
    @Order(10)
    public void testMakerSignal() {
        // Maker signals the payment was received (off-chain)
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("reportComplete"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100).blockingGet();
        BT.forgeBlock(makerPass);
        BT.forgeBlock(makerPass);

        // order should be finished
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        assertEquals(SellContract.STATE_FINISHED, state_chain);

        balance = BT.getContractBalance(contract).longValue();
        assertTrue(balance == 0, "not enough balance");
        assertTrue(accBalance(takerPass) > amount, "taker have not received");
    }

    @Test
    @Order(11)
    public void testReopen(){
        //fund maker if needed
        while(accBalance(makerPass) < amount + security + SellContract.ACTIVATION_FEE){
            BT.forgeBlock(makerPass);
        }
        // Reopen the offer
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(amount + security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                security).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // should now be open
        state = SellContract.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(state, state_chain);
        assertTrue(amount_chain > amount);
        assertEquals(security, security_chain);
    }

    @Test
    @Order(12)
    public void testCancelAndWithdrawOffer() {
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                0).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(SellContract.STATE_FINISHED, state_chain);
        assertEquals(0, amount_chain);
        assertEquals(0, BT.getContractBalance(contract).longValue());
    }
}
