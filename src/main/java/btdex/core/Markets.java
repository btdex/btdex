package btdex.core;

import btdex.markets.*;

import java.util.ArrayList;

public class Markets {
    private static ArrayList<Market> markets = new ArrayList<>();
    private static Market token;

    public static void addMarkets(Boolean testnet) {
        markets.add(token = new MarketTRT());
//        if(testnet) {
//            markets.add(new MarketNDST());
//        }
//        markets.add(new MarketEUR());
//        markets.add(new MarketBRL());
        markets.add(new MarketBTC());
        markets.add(new MarketETH());
        markets.add(new MarketLTC());
        markets.add(new MarketXMR());
    }

    public static ArrayList<Market> getMarkets(){
        return markets;
    }

    public static Market getToken() {
        return token;
    }
}
