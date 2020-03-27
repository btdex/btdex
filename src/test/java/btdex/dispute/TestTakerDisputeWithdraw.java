package btdex.dispute;

import bt.BT;
import btdex.sc.SellContract;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//fails in "fresh" IDE IntelliJ, later passes
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTakerDisputeWithdraw {
    private static InitSC sc;
    private static long state;
    private static long amountToMaker = 0;
    private static long amountToTaker = 10000;

    @Test
    @Order(1)
    public void initSC() {
        sc = new InitSC();

        long state = SellContract.STATE_WAITING_PAYMT;
        long state_chain = sc.getContractFieldValue("state");

        if(state_chain == -1){
            System.out.println("Contract not found");
            return;
        } else if(state_chain == -2){
            System.out.println("Field not found");
            return;
        }
        assertEquals(state, state_chain);

    }

    @Test
    @Order(2)
    public void testTakerDispute() {
        sc.dispute(sc.getTaker(), amountToMaker, amountToTaker);
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_TAKER_DISPUTE;
        assertEquals(state, state&sc.getContractFieldValue("state"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeTakerAmountToTaker"));
    }

    @Test
    @Order(3)
    public void testWithdraw() {
        long makerBalance = sc.getMakerBalance();
        long takerBalance = sc.getTakerBalance();
        sc.withdraw();
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_TAKER_DISPUTE;
        assertEquals(state, state&sc.getContractFieldValue("state"));
        assertTrue(sc.getSCbalance() > 0);
        assertTrue(takerBalance == sc.getTakerBalance());
        assertTrue(sc.getMakerBalance() < makerBalance);
    }
}
