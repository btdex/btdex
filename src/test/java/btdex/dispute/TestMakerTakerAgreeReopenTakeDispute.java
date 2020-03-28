package btdex.dispute;

import bt.BT;
import btdex.sc.SellContract;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

//fails in "fresh" IDE IntelliJ, later passes
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMakerTakerAgreeReopenTakeDispute {
    private static InitSC sc;
    private static long state;
    private static long amountToMaker = 10000;
    private static long amountToTaker = 0;

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
    public void testMakerDispute() {
        sc.dispute(sc.getMaker(), amountToMaker, amountToTaker);
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_CREATOR_DISPUTE;
        assertEquals(state, state&sc.getContractFieldValue("state"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToCreator"));
    }

    @Test
    @Order(3)
    public void testTakerDispute() {
        sc.dispute(sc.getTaker(), amountToMaker, amountToTaker);
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_FINISHED;
        assertEquals(state, sc.getContractFieldValue("state"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeTakerAmountToTaker"));
    }

    @Test
    @Order(4)
    public void testReopen() {
        sc.initOffer();
        BT.forgeBlock();

        state = SellContract.STATE_OPEN;
        assertEquals(state, sc.getContractFieldValue("state"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeTakerAmountToTaker"));

    }

    @Test
    @Order(5)
    public void testTakeOffer() {
        sc.takeOffer();

        state = SellContract.STATE_WAITING_PAYMT;
        assertEquals(state, state&sc.getContractFieldValue("state"));
    }

    @Test
    @Order(6)
    public void testTakerDisputeAgain() {
        long takerBalance = sc.getTakerBalance();
        sc.dispute(sc.getTaker(), amountToMaker, amountToTaker);

        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_TAKER_DISPUTE;
        long state_other = SellContract.STATE_BOTH_DISPUTE;
        assertEquals(state, state&sc.getContractFieldValue("state"));
        assertNotEquals(state_other,state_other&sc.getContractFieldValue("state"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(amountToMaker, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(amountToTaker, sc.getContractFieldValue("disputeTakerAmountToTaker"));
        assertTrue(sc.getSCbalance() > 0, "SC balance 0");
        assertTrue(sc.getTakerBalance() < takerBalance, "Taker balance bad");
    }

}
