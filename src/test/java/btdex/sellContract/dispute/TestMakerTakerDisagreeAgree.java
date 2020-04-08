package btdex.sellContract.dispute;

import bt.BT;
import btdex.sc.SellContract;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

//fails in "fresh" IDE IntelliJ, later passes
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMakerTakerDisagreeAgree {
    private static InitSC sc;
    private static long state;
    private static long disputeCreatorAmountToCreator = 10000;
    private static long disputeTakerAmountToTaker = 10000;

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
        sc.dispute(sc.getMaker(), disputeCreatorAmountToCreator, 0);
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_CREATOR_DISPUTE;
        assertEquals(state, state&sc.getContractFieldValue("state"));
        assertEquals(disputeCreatorAmountToCreator, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToCreator"));
    }

    @Test
    @Order(3)
    public void testTakerDispute() {
        sc.dispute(sc.getTaker(), 0, disputeTakerAmountToTaker);
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_BOTH_DISPUTE;
        assertEquals(state, state&sc.getContractFieldValue("state"));
        assertEquals(disputeCreatorAmountToCreator, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(disputeTakerAmountToTaker, sc.getContractFieldValue("disputeTakerAmountToTaker"));
    }

    @Test
    @Order(3)
    public void testMakerAgree() {
        long feeContractBalance = sc.getFeeContractBalance();
        long takerBalance = sc.getTakerBalance();
        sc.dispute(sc.getMaker(), 0, disputeTakerAmountToTaker);
        BT.forgeBlock();
        BT.forgeBlock();

        state = SellContract.STATE_FINISHED;
        assertEquals(state, sc.getContractFieldValue("state"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(disputeTakerAmountToTaker, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(disputeTakerAmountToTaker, sc.getContractFieldValue("disputeTakerAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(0, sc.getSCbalance());
        assertTrue(takerBalance < sc.getTakerBalance());
        assertTrue(sc.getFeeContractBalance() > feeContractBalance, "FeeContract do not got fee");
    }

}
