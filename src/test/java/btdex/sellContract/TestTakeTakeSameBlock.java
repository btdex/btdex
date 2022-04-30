package btdex.sellContract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import bt.BT;
import btdex.CreateSC;
import btdex.sc.SellContract;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.service.NodeService;

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
    private static SignumAddress takerOne;
    private static SignumAddress takerTwo;
    private static NodeService bns = BT.getNode();
    private static CreateSC sc;

    @Test
    @Order(1)
    public void initSC() throws IOException {
        sc = new CreateSC(SellContract.class, 10000, 100);
        makerPass = Long.toString(System.currentTimeMillis());
        String name = sc.registerSC(makerPass);

        SignumAddress maker = BT.getAddressFromPassphrase(makerPass);
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
                SignumValue.fromNQT(sent), SignumValue.fromSigna(0.1), 1000,
                security);
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());
    }

    private long accBalance(String pass) {
        return (bns.getAccount(BT.getAddressFromPassphrase(pass)).blockingGet()).getBalance().longValue();
    }

    @Test
    @Order(2)
    public void initTakerOne() {
        takerPassOne = Long.toString(System.currentTimeMillis());
        takerOne = BT.getAddressFromPassphrase(takerPassOne);
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
        takerTwo = BT.getAddressFromPassphrase(takerPassTwo);
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
                SignumValue.fromNQT(security + SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 100,
                security, amount_chain);
        BT.callMethod(takerPassTwo, contract.getId(), compiled.getMethod("take"),
                SignumValue.fromNQT(security + SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 100,
                security, amount_chain);
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
