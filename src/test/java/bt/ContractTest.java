package bt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import dex.Exchange;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class ContractTest extends BT {

    public static void main(String[] args) throws Exception {
        ContractTest t = new ContractTest();
        t.testCreate();
    }

    @Test
    public void testCreate() throws Exception {

        long amount = 10000 * Contract.ONE_BURST;

        long rate = 70;
        long pauseTimeout = 100;
        long security = 100 * Contract.ONE_BURST;

        // Create a maker address and send some coins
        String makerPass = Long.toString(System.currentTimeMillis());
        BurstAddress maker = BT.getBurstAddressFromPassphrase(makerPass);

        BT.sendAmount(BT.PASSPHRASE, maker, BurstValue.fromPlanck(2 * amount + 3 * security)).blockingGet();
        BT.forgeBlock();

        BurstCrypto bc = BurstCrypto.getInstance();

        BurstID arbitrator1 = bc.rsDecode("GFP4-TVNR-S7TY-E5KAY");
        BurstID arbitrator2 = bc.rsDecode("TMSU-YBH5-RVC7-6J6WJ");
        long offerType = 100;
        long state = Exchange.STATE_FINISHED;

        long data[] = { arbitrator1.getSignedLongId(), arbitrator2.getSignedLongId(), offerType };

        bt.compiler.Compiler compiled = BT.compileContract(Exchange.class);
        String name = Exchange.class.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(makerPass, compiled.getCode(), name, name, data,
                BurstValue.fromPlanck(Exchange.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled), 1000).blockingGet();
        BT.forgeBlock();

        AT contract = BT.findContract(maker, name);
        System.out.println(contract.getId().getID());

        long arb1_chain = BT.getContractFieldValue(contract, compiled.getField("arbitrator1").getAddress());
        long arb2_chain = BT.getContractFieldValue(contract, compiled.getField("arbitrator2").getAddress());

        long offerType_chain = BT.getContractFieldValue(contract, compiled.getField("offerType").getAddress());
        long state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());

        assertEquals(arbitrator1.getSignedLongId(), arb1_chain);
        assertEquals(arbitrator2.getSignedLongId(), arb2_chain);
        assertEquals(offerType, offerType_chain);
        assertEquals(state, state_chain);

        // Initialize the offer
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("updateOrTake"),
                BurstValue.fromPlanck(amount + security + Exchange.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                rate, security, pauseTimeout).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // should now be open
        state = Exchange.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        long rate_chain = BT.getContractFieldValue(contract, compiled.getField("rate").getAddress());
        long amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        long security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(state, state_chain);
        assertEquals(rate, rate_chain);
        // assertEquals(pauseTimeout, pauseTimeout_chain);
        assertTrue(amount_chain > amount);
        assertEquals(security, security_chain);

        long balance = BT.getContractBalance(contract).longValue();
        assertTrue("not enough balance", balance > amount + security);

        // Taking the offer, create a taker address and send some coins
        String takerPass = Long.toString(System.currentTimeMillis());
        BurstAddress taker = BT.getBurstAddressFromPassphrase(takerPass);

        BT.sendAmount(BT.PASSPHRASE, taker, BurstValue.fromPlanck(2 * security)).blockingGet();
        BT.forgeBlock();

        // Invalid take, not enough security
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("updateOrTake"),
                BurstValue.fromPlanck(Exchange.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100, rate, security, 0)
                .blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // order should not be taken
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        long taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        assertEquals(Exchange.STATE_OPEN, state_chain);
        assertEquals(0, taker_chain);

        // Take the offer
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("updateOrTake"),
                BurstValue.fromPlanck(security + Exchange.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100, rate,
                security, 0).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // order should be taken, waiting for payment
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        assertEquals(Exchange.STATE_WAITING_PAYMT, state_chain);
        assertEquals(taker.getSignedLongId(), taker_chain);

        balance = BT.getContractBalance(contract).longValue();
        assertTrue("not enough balance", balance > amount + security * 2);

        // Maker signals the payment was received (off-chain)
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("reportComplete"),
                BurstValue.fromPlanck(Exchange.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // order should be finished
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        assertEquals(Exchange.STATE_FINISHED, state_chain);

        long taker_balance = BT.getNode().getAccount(taker).blockingGet().getBalance().longValue();

        balance = BT.getContractBalance(contract).longValue();
        assertTrue("not enough balance", balance == 0);
        assertTrue("taker have not received", taker_balance > amount);

        // Reopen the offer
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("updateOrTake"),
                BurstValue.fromPlanck(amount + security + Exchange.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                rate, security, pauseTimeout).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // should now be open
        state = Exchange.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        rate_chain = BT.getContractFieldValue(contract, compiled.getField("rate").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(state, state_chain);
        assertEquals(rate, rate_chain);
        assertTrue(amount_chain > amount);
        assertEquals(security, security_chain);
    }
}
