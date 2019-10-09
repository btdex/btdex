package btdex;

import java.util.HashMap;

import btdex.core.ContractState;
import btdex.core.Market;
import burst.kit.entity.BurstAddress;

public class ListAll {
	
	public static void main(String[] args) {
		HashMap<BurstAddress, ContractState> map = new HashMap<>();
		
		ContractState.addContracts(map);
		
		for (ContractState s : map.values()) {
			if(s.getMarket() == Market.MARKET_BTC)
				System.out.println(s.getAddress());
		}
	}
}
