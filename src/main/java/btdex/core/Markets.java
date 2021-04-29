package btdex.core;

import java.util.ArrayList;

import btdex.markets.MarketARRR;
import btdex.markets.MarketBCH;
import btdex.markets.MarketBHD;
import btdex.markets.MarketBNB;
import btdex.markets.MarketBSV;
import btdex.markets.MarketBTC;
import btdex.markets.MarketDOGE;
import btdex.markets.MarketETH;
import btdex.markets.MarketLTC;
import btdex.markets.MarketTRT;
import btdex.markets.MarketXMR;
import btdex.markets.MarketXLA;
import burst.kit.service.BurstNodeService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Markets {
    private static ArrayList<Market> markets = new ArrayList<>();
    private static ArrayList<Market> userMarkets = new ArrayList<>();
    private static Market token;

    private static Logger logger = LogManager.getLogger();

    public static void loadStandardMarkets(Boolean testnet, BurstNodeService NS) {
        markets.add(token = new MarketTRT());
        
//        markets.add(new MarketEUR());
//        markets.add(new MarketBRL());
        
        markets.add(new MarketBTC());
        markets.add(new MarketARRR());
        markets.add(new MarketBCH());
        markets.add(new MarketBHD());
        markets.add(new MarketBNB());
        markets.add(new MarketBSV());
        markets.add(new MarketDOGE());
        markets.add(new MarketETH());
        markets.add(new MarketLTC());
        markets.add(new MarketXMR());
        markets.add(new MarketXLA());
    }

    public static void addUserMarket(Market m) {
    	// Add after TRT in the beginning of the list
    	markets.add(userMarkets.size()+1, m);
    	userMarkets.add(m);
		logger.info("User market {} added", m.getID());
    }

    public static void removeUserMarket(Market m) {
    	markets.remove(m);
    	userMarkets.remove(m);
		logger.info("User market {} removed", m.getID());
    }

    public static ArrayList<Market> getMarkets(){
        return markets;
    }

    public static ArrayList<Market> getUserMarkets(){
        return userMarkets;
    }

    public static Market getToken() {
        return token;
    }

    public static Market findMarket(long id) {
    	for(Market m : markets) {
    		if(m.getID() == id)
    			return m;
    	}
    	return null;
    }
}
