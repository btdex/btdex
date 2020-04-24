package btdex.core;

import java.util.ArrayList;

import btdex.markets.MarketBTC;
import btdex.markets.MarketDOGE;
import btdex.markets.MarketETH;
import btdex.markets.MarketLTC;
import btdex.markets.MarketTRT;
import btdex.markets.MarketXMR;
import burst.kit.service.BurstNodeService;

public class Markets {
    private static ArrayList<Market> markets = new ArrayList<>();
    private static ArrayList<Market> userMarkets = new ArrayList<>();
    private static Market token;

    public static void loadStandardMarkets(Boolean testnet, BurstNodeService NS) {
        markets.add(token = new MarketTRT());
//        markets.add(new MarketEUR());
//        markets.add(new MarketBRL());
        markets.add(new MarketBTC());
        markets.add(new MarketETH());
        markets.add(new MarketLTC());
        markets.add(new MarketXMR());
        markets.add(new MarketDOGE());
    }
    
    public static void addUserMarket(Market m) {
    	markets.add(m);
    	userMarkets.add(m);
    }
    
    public static void removeUserMarket(Market m) {
    	markets.remove(m);
    	userMarkets.remove(m);
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
}
