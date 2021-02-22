package btdex;

import bt.BT;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Account;

public class Balances {
	
	public static void main(String[] args) {
		BT.setNodeAddress(BT.NODE_BURSTCOIN_RO);

		BurstAddress ad = BurstAddress.fromEither("BURST-BTKF-8WT9-L98N-98JH2");
		
		Account ac = BT.getNode().getAccount(ad, null, null).blockingGet();
		
		System.out.println(ac.getBalance().longValue());
		System.out.println(ac.getUnconfirmedBalance().longValue());
	}
}
