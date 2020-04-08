package btdex.sellContract.dispute;

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
public class TestTakerDisputeReportComplete {
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
    public void testInvalidReportComplete() {
        sc.complete(sc.getTaker());
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
    @Order(4)
    public void testValidReportComplete() {
        long feeContractBalance = sc.getFeeContractBalance();
        long makerBalance = sc.getMakerBalance();
        long takerBalance = sc.getTakerBalance();
        sc.complete(sc.getMaker());
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_FINISHED;
        assertEquals(state, state&sc.getContractFieldValue("state"));
        assertEquals(0, sc.getSCbalance());
        //taker gets more, because SellContract.ACTIVATION_FEE not used all
        assertTrue(takerBalance + sc.getAmount() < sc.getTakerBalance());
        assertTrue(sc.getMakerBalance() > makerBalance, "Maker do not received security payment");
        assertTrue(sc.getFeeContractBalance() > feeContractBalance, "FeeContract do not got fee");
    }
}
