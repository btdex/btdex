package btdex;

import bt.BT;
import bt.Contract;
import btdex.core.Mediators;
import btdex.sc.SellContract;
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
		
		BT.setNodeAddress(BT.NODE_LOCAL_TESTNET);

		long amount = 1000 * Contract.ONE_BURST;

		long security = 100 * Contract.ONE_BURST;

		BurstID feeContract = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE).getBurstID();
		Mediators mediators = new Mediators(true);
		BurstID mediator1 = mediators.getMediators()[0];
		BurstID mediator2 = mediators.getMediators()[1];

		long data[] = {
				feeContract.getSignedLongId(),
				mediator1.getSignedLongId(), mediator2.getSignedLongId(),
		};

		bt.compiler.Compiler compiled = BT.compileContract(SellContract.class);
		System.out.println("Contract compiled..");
		for (int i = 0; i < 4; i++) {
			String name = SellContract.class.getSimpleName() + System.currentTimeMillis();
			System.out.println("Contracts name: "+ name);
			BT.registerContract(BT.PASSPHRASE2, compiled.getCode(), compiled.getDataPages(), name, name, data,
					BurstValue.fromPlanck(SellContract.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled), 1000).blockingGet();
			System.out.println("Contract registered");
			BT.forgeBlock(BT.PASSPHRASE2);

			AT contract = BT.findContract(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2), name);
			System.out.println("Contract found on chain! It's id " + contract.getId().getID());

			// Initialize the offer
			BT.callMethod(
					BT.PASSPHRASE2,
					contract.getId(),
					compiled.getMethod("update"),
					BurstValue.fromPlanck(amount + security + SellContract.ACTIVATION_FEE),
					BurstValue.fromBurst(0.1),
					100,
					security
					).blockingGet();
			BT.forgeBlock();
			BT.forgeBlock();

			amount *= 2;
			security *= 2;
		}

	}

}
