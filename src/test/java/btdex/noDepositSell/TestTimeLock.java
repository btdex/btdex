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
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;

//This is long test, about 5 min
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTimeLock {
    private static AT contract;
    private static CreateNoDepositSC sc;
    private static bt.compiler.Compiler compiled;
    private static long state_chain;
    private static long state;
    private static String makerPass;
    private static int block = 4; //1 block 4 min
    private static int day = 360;

    @Test
    @Order(1)
    public void initSC() throws IOException {
        sc = new CreateNoDepositSC(block * day);
        makerPass = Long.toString(System.currentTimeMillis());
        sc.registerSC(makerPass);

        BurstAddress maker = BT.getBurstAddressFromPassphrase(makerPass);
        BT.forgeBlock();
        compiled = sc.getCompiled();
        contract = BT.findContract(maker, sc.getName());

        BurstValue send = BurstValue.fromBurst(100);
        BT.sendAmount(makerPass, contract.getId(), send, BurstValue.fromBurst(0.1)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("withdraw"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock(); //1
        BT.forgeBlock(); //2

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_WITHDRAW_REQUESTED;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(2)
    public void withdrawSignalToEarly() {
        long SCbalanceBefore = BT.getContractBalance(contract).longValue();
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("withdraw"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock(); //3
        BT.forgeBlock(); //4

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_WITHDRAW_REQUESTED;
        assertEquals(state, state_chain, "State not equal");
        long SCbalance = BT.getContractBalance(contract).longValue();
        assertTrue(SCbalanceBefore < SCbalance);
    }

    @Test
    @Order(3)
    public void withdrawSignalToEarlyAgain() {
        for(int i = 5; i < 101; i++){ //after 100 blocks
            BT.forgeBlock();
        }
        long SCbalanceBefore = BT.getContractBalance(contract).longValue();
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("withdraw"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_WITHDRAW_REQUESTED;
        assertEquals(state, state_chain, "State not equal");
        long SCbalance = BT.getContractBalance(contract).longValue();
        assertTrue(SCbalanceBefore < SCbalance);
    }

    @Test
    @Order(4)
    public void withdrawValidSignal() {
        for(int i = 103; i < day + 1; i++){ //after day
            BT.forgeBlock();
        }

        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("withdraw"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_OPEN;
        assertEquals(state, state_chain, "State not equal");
        long SCbalance = BT.getContractBalance(contract).longValue();
        assertEquals(0, SCbalance);
    }
}
