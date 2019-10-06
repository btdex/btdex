package btdex;

import bt.BT;
import bt.Contract;
import btdex.core.Globals;
import btdex.sm.SellContract;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class PopulateMarket extends BT {

	public static void main(String[] args) throws Exception {

		long amount = 10000 * Contract.ONE_BURST;
		long rate = 70;
		long pauseTimeout = 100;
		long security = 100 * Contract.ONE_BURST;

		BurstID arbitrator1 = Globals.ARBITRATORS[0];
		BurstID arbitrator2 = Globals.ARBITRATOR_BAKCUP;
		long offerType = Globals.MARKET_BTC;

		long data[] = { arbitrator1.getSignedLongId(), arbitrator2.getSignedLongId(), offerType };

		bt.compiler.Compiler compiled = BT.compileContract(SellContract.class);

		for (int i = 0; i < 4; i++) {
			String name = SellContract.class.getSimpleName() + System.currentTimeMillis();

			BT.registerContract(BT.PASSPHRASE2, compiled.getCode(), name, name, data,
					BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled), 1000).blockingGet();
			BT.forgeBlock();

			AT contract = BT.findContract(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2), name);
			System.out.println(contract.getId().getID());

			// Initialize the offer
			BT.callMethod(BT.PASSPHRASE2, contract.getId(), compiled.getMethod("update"),
					BurstValue.fromPlanck(amount + security + SellContract.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 100,
					rate, security, pauseTimeout).blockingGet();
			BT.forgeBlock();
			BT.forgeBlock();

			rate *= 2;
			amount *= 2;
			security *= 2;
		}
	}
}
