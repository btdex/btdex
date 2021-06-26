package btdex;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import btdex.core.ContractState;
import btdex.core.Market;
import burst.kit.entity.BurstAddress;

public class ListAll {
	
	public static void main(String[] args) {
		ConcurrentHashMap<BurstAddress, ContractState> map = new ConcurrentHashMap<>();
		
		ContractState.addContracts(map, null);
		
		for (ContractState s : map.values()) {
			if(s.getMarket() == Market.MARKET_BTC)
				System.out.println(s.getAddress());
		}
	}
}
