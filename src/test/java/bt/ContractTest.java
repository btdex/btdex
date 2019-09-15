package bt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import burst.kit.crypto.BurstCrypto;
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

    public void testCreate() throws Exception {
        BT.forgeBlock();

        BurstCrypto bc = BurstCrypto.getInstance();
        long amount = 1000 * Contract.ONE_BURST;

        BurstID arbitrator1 = bc.rsDecode("TMSU-YBH5-RVC7-6J6WJ");
        BurstID arbitrator2 = bc.rsDecode("TMSU-YBH5-RVC7-6J6WJ");
        long offerType = 100;
        long state = Exchange.STATE_FINISHED;

        long rate = 70;
        long pauseTimeout = 1000;
        long security = 100 * Contract.ONE_BURST;

        long data[] = {
            arbitrator1.getSignedLongId(),
            arbitrator2.getSignedLongId(),
            offerType,
        };

        bt.compiler.Compiler compiled = BT.compileContract(Exchange.class);
        String name = Exchange.class.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(BT.PASSPHRASE, compiled.getCode(), name, name, data,
                BurstValue.fromPlanck(Exchange.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled), 1000).blockingGet();
        BT.forgeBlock();
        
        AT contract = BT.findContract(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE), name);
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
        BT.callMethod(BT.PASSPHRASE, contract.getId(), compiled.getMethod("updateOrTake"),
            BurstValue.fromPlanck(amount + security + Exchange.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
            rate, security, pauseTimeout).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // should now be open
        state = Exchange.STATE_OPEN;
        state_chain = BT.getContractFieldValue(contract, compiled.getField("state").getAddress());
        long rate_chain = BT.getContractFieldValue(contract, compiled.getField("rate").getAddress());
        long pauseTimeout_chain = BT.getContractFieldValue(contract, compiled.getField("pauseTimeout").getAddress());
        long amount_chain = BT.getContractFieldValue(contract, compiled.getField("amount").getAddress());
        long security_chain = BT.getContractFieldValue(contract, compiled.getField("security").getAddress());

        assertEquals(rate, rate_chain);
        //assertEquals(pauseTimeout, pauseTimeout_chain);
        assertTrue(amount_chain > amount);
        assertEquals(security, security_chain);

        long balance = BT.getContractBalance(contract).longValue();
        assertTrue("not enough balance", balance > (amount + security));
    }
}
