package btdex;

import bt.BT;
import signumj.entity.SignumAddress;
import signumj.entity.response.Account;

public class Balances {
	
	public static void main(String[] args) {
		BT.setNodeAddress(BT.NODE_BURSTCOIN_RO);

		SignumAddress ad = SignumAddress.fromEither("S-BTKF-8WT9-L98N-98JH2");
		
		Account ac = BT.getNode().getAccount(ad).blockingGet();
		
		System.out.println(ac.getBalance().longValue());
		System.out.println(ac.getUnconfirmedBalance().longValue());
	}
}
