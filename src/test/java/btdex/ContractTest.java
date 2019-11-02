package btdex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import bt.BT;
import bt.Contract;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.sm.SellContract;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;

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
        long security = 100 * Contract.ONE_BURST;

        // Create a maker address and send some coins
        String makerPass = Long.toString(System.currentTimeMillis());
        BurstAddress maker = BT.getBurstAddressFromPassphrase(makerPass);
        BurstAddress feeContract = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE);

        BT.sendAmount(BT.PASSPHRASE, maker, BurstValue.fromPlanck(2 * amount + 3 * security)).blockingGet();
        BT.forgeBlock();

        BurstID arbitrator1 = Globals.MEDIATORS[0];
        BurstID arbitrator2 = Globals.MEDIATORS[1];
        long offerType = Market.MARKET_BTC;
        long state = SellContract.STATE_FINISHED;

        long data[] = { feeContract.getSignedLongId(), arbitrator1.getSignedLongId(), arbitrator2.getSignedLongId(), offerType };

        bt.compiler.Compiler compiled = BT.compileContract(SellContract.class);
        String name = SellContract.class.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(makerPass, compiled.getCode(), compiled.getDataPages(), name, name, data,
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled), 1000).blockingGet();
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
        long sent = amount + security + SellContract.ACTIVATION_FEE;
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(sent), BurstValue.fromBurst(0.1), 100,
                rate, security).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // should now be open
        state = SellContract.STATE_OPEN;
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
        System.out.println("Contract fees for update: " + BurstValue.fromPlanck(sent-balance));
        assertTrue("not enough balance", balance > amount + security);

        // Taking the offer, create a taker address and send some coins
        String takerPass = Long.toString(System.currentTimeMillis());
        BurstAddress taker = BT.getBurstAddressFromPassphrase(takerPass);

        BT.sendAmount(BT.PASSPHRASE, taker, BurstValue.fromPlanck(2 * security)).blockingGet();
        BT.forgeBlock();

        // Invalid take, not enough security
        sent = balance + SellContract.ACTIVATION_FEE*2;
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE*2), BurstValue.fromBurst(0.1), 100, rate, security, amount_chain)
                .blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        balance = BT.getContractBalance(contract).longValue();
        System.out.println("Contract fees for failed take: " + BurstValue.fromPlanck(sent-balance-SellContract.ACTIVATION_FEE));

        // order should not be taken
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        long taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        assertEquals(SellContract.STATE_OPEN, state_chain);
        assertEquals(0, taker_chain);

        sent = balance + security + SellContract.ACTIVATION_FEE;
        // Take the offer
        BT.callMethod(takerPass, contract.getId(), compiled.getMethod("take"),
                BurstValue.fromPlanck(security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                rate, security, amount_chain).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // order should be taken, waiting for payment
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        taker_chain = BT.getContractFieldValue(contract, compiled.getField("taker").getAddress());

        assertEquals(SellContract.STATE_WAITING_PAYMT, state_chain);
        assertEquals(taker.getSignedLongId(), taker_chain);

        balance = BT.getContractBalance(contract).longValue();
        assertTrue("not enough balance", balance > amount + security * 2);
        System.out.println("Contract fees to take: " + BurstValue.fromPlanck(sent-balance));

        // Maker signals the payment was received (off-chain)
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("reportComplete"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // order should be finished
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        assertEquals(SellContract.STATE_FINISHED, state_chain);

        long taker_balance = BT.getNode().getAccount(taker).blockingGet().getBalance().longValue();

        balance = BT.getContractBalance(contract).longValue();
        assertTrue("not enough balance", balance == 0);
        assertTrue("taker have not received", taker_balance > amount);

        // Reopen the offer
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(amount + security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                rate, security).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // should now be open
        state = SellContract.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        rate_chain = BT.getContractFieldValue(contract, compiled.getField("rate").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(state, state_chain);
        assertEquals(rate, rate_chain);
        assertTrue(amount_chain > amount);
        assertEquals(security, security_chain);

        // Cancel and withdraw the offer
        BT.callMethod(makerPass, contract.getId(), compiled.getMethod("update"),
                BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
                rate, 0).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        rate_chain = BT.getContractFieldValue(contract, compiled.getField("rate").getAddress());
        amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(SellContract.STATE_FINISHED, state_chain);
        assertEquals(0, amount_chain);
        assertEquals(0, BT.getContractBalance(contract).longValue());
    }
}
