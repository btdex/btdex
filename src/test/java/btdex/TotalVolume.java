package btdex;

import bt.BT;
import btdex.core.NumberFormatting;
import signumj.entity.SignumID;
import signumj.entity.response.AssetTrade;

/**
 * Accounts for all TRT transactions so far.
 * 
 * @author jjos
 *
 */
public class TotalVolume {
	
	public static void main(String[] args) {
		BT.setNodeAddress(BT.NODE_BURSTCOIN_RO);
		
		int start = 0;
		int N = 200;
		long quantity = 0;
		long volume = 0;
		while (true) {
			AssetTrade[] trades = BT.getNode().getAssetTrades(SignumID.fromLong("12402415494995249540"), null, start, start+N).blockingGet();
			if(trades.length == 0)
				break;
			
			for(AssetTrade t : trades) {
				quantity += t.getQuantity().longValue();
				volume += t.getQuantity().longValue() * t.getPrice().longValue();
			}
			
			start += N+1;
		}
		
		System.out.println("Total TRT/BURST volume on BTDEX:");
		System.out.println(NumberFormatting.TOKEN.format(quantity/10000.0) + " TRT");
		System.out.println(NumberFormatting.BURST.format(volume) + " BURST");
	}
}
