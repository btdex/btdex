package btdex;

import java.util.concurrent.ConcurrentHashMap;

import btdex.core.ContractState;
import btdex.core.Market;
import signumj.entity.SignumAddress;

public class ListAll {
	
	public static void main(String[] args) {
		ConcurrentHashMap<SignumAddress, ContractState> map = new ConcurrentHashMap<>();
		
		ContractState.addContracts(map);
		
		for (ContractState s : map.values()) {
			if(s.getMarket() == Market.MARKET_BTC)
				System.out.println(s.getAddress());
		}
	}
}
