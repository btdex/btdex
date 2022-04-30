package btdex.noDepositSell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import bt.BT;
import btdex.sc.SellContract;
import btdex.sc.SellNoDepositContract;
import signumj.entity.SignumAddress;
import signumj.entity.SignumID;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.TransactionBroadcast;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 *
 * @author shefas
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCreationWithdraw {
    private static AT contract;
    private static CreateNoDepositSC sc;
    private static bt.compiler.Compiler compiled;
    private static long state_chain;
    private static long state;
    private static String makerPass;
    private static int block = 4; //1 block 4 min

    @Test
    @Order(1)
    public void initSC() throws IOException {
        sc = new CreateNoDepositSC(block * 3);
        makerPass = Long.toString(System.currentTimeMillis());
        sc.registerSC(makerPass);

        SignumAddress maker = BT.getAddressFromPassphrase(makerPass);
        BT.forgeBlock();
        compiled = sc.getCompiled();
        contract = BT.findContract(maker, sc.getName());
    }

    @Test
    @Order(2)
    public void testMediators() {
        SignumID mediator1 = sc.getMediator1();
        SignumID mediator2 = sc.getMediator2();
        long med1_chain = BT.getContractFieldValue(contract, compiled.getField("mediator1").getAddress());
        long med2_chain = BT.getContractFieldValue(contract, compiled.getField("mediator2").getAddress());

        assertEquals(mediator1.getSignedLongId(), med1_chain, "Mediator1 not equal");
        assertEquals(mediator2.getSignedLongId(), med2_chain, "Mediator2 not equal");
    }

    @Test
    @Order(3)
    public void testState(){
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_FINISHED;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(4)
    public void sendInvalidCoin(){
        SignumValue send = SignumValue.fromSigna(200);
        //sender is not a contract creator
        TransactionBroadcast tb = BT.sendAmount(BT.PASSPHRASE, contract.getId(), send, SignumValue.fromSigna(0.1));
        BT.forgeBlock(tb);
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_FINISHED;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(5)
    public void sendCoin(){
        long SCbalanceBefore = BT.getContractBalance(contract).longValue();
        SignumValue send = SignumValue.fromSigna(100);
        TransactionBroadcast tb = BT.sendAmount(makerPass, contract.getId(), send, SignumValue.fromSigna(0.1));
        BT.forgeBlock(tb);
        BT.forgeBlock();

        long SCbalance = BT.getContractBalance(contract).longValue();
        assertTrue(SCbalanceBefore < SCbalance);
    }

    @Test
    @Order(6)
    public void testStateAgain(){
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_OPEN;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(7)
    public void withdrawSignal() {
    	TransactionBroadcast tb = BT.callMethod(makerPass, contract.getId(), compiled.getMethod("withdraw"),
                SignumValue.fromNQT(SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000);
        BT.forgeBlock(tb);
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_WITHDRAW_REQUESTED;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(8)
    public void withdrawSignalToEarly() {
        //two blocks passed
        long SCbalanceBefore = BT.getContractBalance(contract).longValue();
        TransactionBroadcast tb = BT.callMethod(makerPass, contract.getId(), compiled.getMethod("withdraw"),
                SignumValue.fromNQT(SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000);
        BT.forgeBlock(tb);
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_WITHDRAW_REQUESTED;
        assertEquals(state, state_chain, "State not equal");
        long SCbalance = BT.getContractBalance(contract).longValue();
        assertTrue(SCbalanceBefore < SCbalance);
    }

    @Test
    @Order(9)
    public void withdrawSignalValid() {
    	TransactionBroadcast tb = BT.callMethod(makerPass, contract.getId(), compiled.getMethod("withdraw"),
                SignumValue.fromNQT(SellContract.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000);
        BT.forgeBlock(tb);
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_OPEN;
        assertEquals(state, state_chain, "State not equal");
        long SCbalance = BT.getContractBalance(contract).longValue();
        assertEquals(0, SCbalance);
    }
}
