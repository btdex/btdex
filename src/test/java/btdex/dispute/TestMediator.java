package btdex.dispute;

import bt.BT;
import btdex.sc.SellContract;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//Not sure why test fails sometimes then you run them in "fresh" IDE
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMediator {
    private static InitSC sc;
    private static long state;
    private static long amountToMaker = 0;
    private static long amountToTaker = 10000;

    @Test
    @Order(1)
    public void initSC() {
        sc = new InitSC();

        state = SellContract.STATE_WAITING_PAYMT;
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
    public void testMediatorAndFeeFields(){
        assertEquals(sc.getMediatorOne().getSignedLongId(), sc.getContractFieldValue("mediator1"));
        assertEquals(sc.getMediatorTwo().getSignedLongId(), sc.getContractFieldValue("mediator2"));
        assertEquals(sc.getFeeContract(), sc.getContractFieldValue("feeContract"));
    }

    @Test
    @Order(3)
    public void testInvalidMediatorDispute() {
        //fund acc
        BT.forgeBlock(BT.PASSPHRASE);
        sc.dispute(sc.getMediatorOne(), amountToMaker, amountToTaker);

        state = SellContract.STATE_WAITING_PAYMT;
        assertEquals(state, sc.getContractFieldValue("state"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToTaker"));
    }
    @Test
    @Order(4)
    public void testInvalidMediatorDisputeAgain() {
        //fund acc
        BT.forgeBlock(BT.PASSPHRASE2);
        sc.dispute(sc.getMediatorTwo(), amountToMaker, amountToTaker);

        state = SellContract.STATE_WAITING_PAYMT;
        assertEquals(state, sc.getContractFieldValue("state"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToTaker"));
    }
    @Test
    @Order(5)
    public void testInvalidMediatorDisputeTogether() {
        sc.dispute(sc.getMediatorOne(), amountToMaker, amountToTaker);
        sc.dispute(sc.getMediatorTwo(), amountToMaker, amountToTaker);

        state = SellContract.STATE_WAITING_PAYMT;
        assertEquals(state, sc.getContractFieldValue("state"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeCreatorAmountToTaker"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToCreator"));
        assertEquals(0, sc.getContractFieldValue("disputeTakerAmountToTaker"));
    }

    @Test
    @Order(6)
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
    @Order(7)
    public void testValidMediatorDispute() {
        long feeContractBalance = sc.getFeeContractBalance();
        long takerBalance = sc.getTakerBalance();
        sc.dispute(sc.getMediatorOne(), amountToMaker, amountToTaker);

        state = SellContract.STATE_FINISHED;
        assertEquals(state, sc.getContractFieldValue("state"));
        assertTrue(sc.getTakerBalance() > takerBalance);
        assertTrue(sc.getFeeContractBalance() > feeContractBalance, "FeeContract do not got fee");
    }

}
