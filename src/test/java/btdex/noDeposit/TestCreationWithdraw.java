package btdex.noDeposit;


import bt.BT;
import bt.Contract;
import btdex.sc.SellContract;
import btdex.sc.SellNoDepositContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.service.BurstNodeService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private static BurstNodeService bns = BT.getNode();

    @Test
    @Order(1)
    public void initSC() throws IOException {
        sc = new CreateNoDepositSC(1);
        makerPass = Long.toString(System.currentTimeMillis());
        sc.registerSC(makerPass);

        BurstAddress maker = BT.getBurstAddressFromPassphrase(makerPass);
        BT.forgeBlock();
        compiled = sc.getCompiled();
        contract = BT.findContract(maker, sc.getName());
        System.out.println("Created contract id " + contract.getId().getID());
        System.out.println("Contracts balance: " + contract.getBalance());
        System.out.println("Total used for registration: " + (SellNoDepositContract.ACTIVATION_FEE - contract.getBalance().longValue()));
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
    public void testState(){
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_FINISHED; //must be open?
        assertEquals(state, state_chain, "State not equal");
    }

    private long accBalance(String pass) {
        return (bns.getAccount(BT.getBurstAddressFromPassphrase(pass)).blockingGet()).getBalance().longValue();
    }

    @Test
    @Order(4)
    public void sendCoin(){
        BurstValue send = BurstValue.fromBurst(100);
        BT.sendAmount(makerPass, contract.getId(), send, BurstValue.fromBurst(0.1)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        long SCbalance = BT.getContractBalance(contract).longValue();
        System.out.println("Sending coins, used by SC: " + BurstValue.fromPlanck(send.longValue() - SCbalance));

    }

    @Test
    @Order(5)
    public void testStateAgain(){
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_FINISHED; //must be open?
        assertEquals(state, state_chain, "State not equal");
    }
}
