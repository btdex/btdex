package btdex.noDepositSell;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestDispute {
    private static AT contract;
    private static CreateNoDepositSC sc;
    private static bt.compiler.Compiler compiled;
    private static long state_chain;
    private static long state;
    private static String makerPass;
    private static int block = 4;

    @Test
    @Order(1)
    public void initSC() throws IOException {
        sc = new CreateNoDepositSC(block * 10);
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

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_OPEN;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(2)
    public void invalidDispute() {
        BT.callMethod(BT.PASSPHRASE3, contract.getId(), compiled.getMethod("dispute"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000, true, 1000000).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_OPEN;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(3)
    public void dispute() {
        BT.callMethod(sc.getMediatorOnePassword(), contract.getId(), compiled.getMethod("dispute"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000, true, 0).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_DISPUTE;
        assertEquals(state, state_chain, "State not equal");
    }

    @Test
    @Order(4)
    public void disputeAgain() {
        long balanceBefore = sc.getFeeContractBalance();
        BT.callMethod(sc.getMediatorTwoPassword(), contract.getId(), compiled.getMethod("dispute"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000, false, 20000000).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        state = SellNoDepositContract.STATE_OPEN;
        assertEquals(state, state_chain, "State not equal");
        long balanceAfter = sc.getFeeContractBalance();
        assertEquals(balanceBefore + 20000000, balanceAfter);
    }
}
