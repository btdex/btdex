package btdex;

import bt.BT;
import btdex.sc.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.service.BurstNodeService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/* This test checks who has priority then taking offer in same time (same block */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTakeTakeSameBlock {
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
    private static String takerPassOne;
    private static String takerPassTwo;
    private static BurstAddress takerOne;
    private static BurstAddress takerTwo;
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

        while(accBalance(makerPass) < sent){
            BT.forgeBlock(makerPass);
        }
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(sent), BurstValue.fromBurst(0.1), 1000,
                security).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());
    }

    private long accBalance(String pass) {
        return (bns.getAccount(BT.getBurstAddressFromPassphrase(pass)).blockingGet()).getBalance().longValue();
    }

    @Test
    @Order(2)
    public void initTakerOne() {
        takerPassOne = Long.toString(System.currentTimeMillis());
        takerOne = BT.getBurstAddressFromPassphrase(takerPassOne);
        //register taker in chain
        BT.forgeBlock(takerPassOne);
        //fund taker if needed
        while (accBalance(takerPassOne) < security + SellContract.ACTIVATION_FEE) {
            BT.forgeBlock(takerPassOne);
        }
    }

    @Test
    @Order(3)
    public void initTakerTwo() {
        takerPassTwo = Long.toString(System.currentTimeMillis());
        takerTwo = BT.getBurstAddressFromPassphrase(takerPassTwo);
        //register taker in chain
        BT.forgeBlock(takerPassTwo);
        //fund taker if needed
        while (accBalance(takerPassTwo) < security + SellContract.ACTIVATION_FEE) {
            BT.forgeBlock(takerPassTwo);
        }
    }
    @Test
    @Order(4)
    public void testOfferTake() {
        // Take the offer
        BT.callMethod(takerPassOne, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                security, amount_chain).blockingGet();
        BT.callMethod(takerPassTwo, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                security, amount_chain).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // order should be taken, waiting for payment
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        state = SellContract.STATE_WAITING_PAYMT;
        assertEquals(state, state_chain);
        //assertEquals(taker.getSignedLongId(), taker_chain);
        System.out.println("Chain: " + taker_chain + " TakerOne " + takerOne.getSignedLongId() +
                " TakerTwo " + takerTwo.getSignedLongId());
    }
}
