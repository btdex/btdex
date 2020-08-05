package btdex.core;

import java.util.ArrayList;

import btdex.markets.MarketBTC;
import btdex.markets.MarketDOGE;
import btdex.markets.MarketETH;
import btdex.markets.MarketLTC;
import btdex.markets.MarketTRT;
import btdex.markets.MarketXMR;
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
        logger.info("TRT market loaded");
//        markets.add(new MarketEUR());
//        markets.add(new MarketBRL());
        markets.add(new MarketBTC());
		logger.info("BTC market loaded");
        markets.add(new MarketETH());
		logger.info("ETH market loaded");
        markets.add(new MarketLTC());
		logger.info("LTC market loaded");
        markets.add(new MarketXMR());
		logger.info("XMR market loaded");
        markets.add(new MarketDOGE());
		logger.info("DOGE market loaded");
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
