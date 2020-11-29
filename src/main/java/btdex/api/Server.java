package btdex.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.ContractTrade;
import btdex.core.ContractType;
import btdex.core.Contracts;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.sc.SellContract;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.AssetTrade;
import burst.kit.entity.response.AssetTrade.TradeType;
import fi.iki.elonen.NanoHTTPD;

/**
 * API with endpoints as recommended by coinmarketcap
 * 
 * @author jjos
 *
 */
public class Server extends NanoHTTPD {
    private static final Logger logger = LogManager.getLogger();;
    
    private static final String API_PREFIX = "/api/v1/";

    public Server(int port) {
        super(port);
        try {
			start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	        logger.info("API server started at port {}", port);
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage());
		}
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String json = null;
            if (session.getUri().startsWith(API_PREFIX)) {
                json = handleApiCall(session.getUri(), session.getParameters());
            }
            if(json == null){
            	// Not found (404)
                return super.serve(session);
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            logger.warn("Error getting response", e);
            return NanoHTTPD.newFixedLengthResponse(e.getMessage());
        }
    }

    private String handleApiCall(String uri, Map<String, List<String>> params) {
    	logger.trace("api call uri: {}", uri);

    	// Support both characters
    	uri = uri.replace('-', '_');
    	
    	BurstNode bn = BurstNode.getInstance();
    	String ret = null;
        
        if (uri.startsWith(API_PREFIX + "summary")) {
            JsonArray pairsJson = new JsonArray();
            
            for(Market m : Markets.getMarkets()) {
            	JsonObject marketJson = new JsonObject();
            	marketJson.addProperty("trading_pairs", m.getTokenID() != null ? Constants.BURST_TICKER + "_"  + m.toString() :
            		m.toString() + "_" + Constants.BURST_TICKER);
            	marketJson.addProperty("base_currency", m.getTokenID() != null ? Constants.BURST_TICKER : m.toString());
            	marketJson.addProperty("quote_currency", m.getTokenID() != null ? m.toString() : Constants.BURST_TICKER);

            	if(m.getTokenID()!=null) {
            		AssetTrade[] trades = bn.getAssetTrades(m);
            		if(trades != null && trades.length > 0)
            			marketJson.addProperty("last_price", trades[0].getPrice().multiply(m.getFactor()).doubleValue());
            		
            		AssetOrder[] asks = bn.getAssetAsks(m);
            		if(asks != null && asks.length > 0)
            			marketJson.addProperty("lowest_ask", asks[0].getPrice().multiply(m.getFactor()).doubleValue());
            		AssetOrder[] bids = bn.getAssetBids(m);
            		if(bids != null && bids.length > 0)
            			marketJson.addProperty("highest_bid", bids[0].getPrice().multiply(m.getFactor()).doubleValue());
            		
                	marketJson.addProperty("base_volume", bn.getBaseVolume24h(m).multiply(m.getFactor()).doubleValue());
                	marketJson.addProperty("quote_volume", bn.getQuoteVolume24h(m).doubleValue());
                	marketJson.addProperty("price_change_percent_24h", bn.getPriceChangePerc24h(m));
                	marketJson.addProperty("highest_price_24h", bn.getHighestPrice24h(m).multiply(m.getFactor()).doubleValue());
                	marketJson.addProperty("lowest_price_24h", bn.getLowestPrice24h(m).multiply(m.getFactor()).doubleValue());
            	}
            	else {
            		// TODO cross-chain
            	}
            	pairsJson.add(marketJson);
            }
            ret = pairsJson.toString();
        }
        else if (uri.startsWith(API_PREFIX + "ticker")) {
        	JsonObject retJson = new JsonObject();
        	
            for(Market m : Markets.getMarkets()) {
            	String pair = m.getTokenID() != null ? Constants.BURST_TICKER + "_" + m.toString() : m.toString() + "_" + Constants.BURST_TICKER;
            	
            	JsonObject marketJson = new JsonObject();
            	
            	if(m.getTokenID() != null) {
            		marketJson.addProperty("base_id", Market.UCA_ID_BURST);
            	}
            	else {
            		marketJson.addProperty("quote_id", Market.UCA_ID_BURST);
            		if(m.getUCA_ID() > 0)
            			marketJson.addProperty("base_id", m.getUCA_ID());
            	}
            	
            	if(m.getTokenID()!=null) {
            		AssetTrade[] trades = bn.getAssetTrades(m);
            		if(trades != null && trades.length > 0) {
            			marketJson.addProperty("last_price", trades[0].getPrice().multiply(m.getFactor()).doubleValue());
                    	marketJson.addProperty("is_frozen", 0);
            		}
            		else {
                    	marketJson.addProperty("is_frozen", 1);
            		}
                	marketJson.addProperty("base_volume", bn.getBaseVolume24h(m).multiply(m.getFactor()).doubleValue());
                	marketJson.addProperty("quote_volume", bn.getQuoteVolume24h(m).doubleValue());
            	}
            	else {
            		// TODO cross-chain
            	}
            	retJson.add(pair, marketJson);
            }
            ret = retJson.toString();
        }
        else if (uri.startsWith(API_PREFIX + "orderbook")) {
            for(Market m : Markets.getMarkets()) {
            	String pair = m.getTokenID() != null ? Constants.BURST_TICKER + "_" + m.toString() : m.toString() + "_" + Constants.BURST_TICKER;

            	if(uri.endsWith(pair)) {
            		JsonObject retJson = new JsonObject();
            		// FIXME get the actual time
            		retJson.addProperty("timestamp", System.currentTimeMillis());
                    
                    JsonArray asksJson = new JsonArray();
                    JsonArray bidsJson = new JsonArray();
                    
            		if(m.getTokenID() != null) {
            			AssetOrder[] asks = bn.getAssetAsks(m);
            			AssetOrder[] bids = bn.getAssetBids(m);
            			
            			for(AssetOrder o : asks) {
            				JsonArray askJson = new JsonArray();
            				// array with price and quantity
            				askJson.add(o.getPrice().multiply(m.getFactor()).doubleValue());
            				askJson.add(o.getQuantity().multiply(m.getFactor()).doubleValue());
            				
            				asksJson.add(askJson);
            			}
            			for(AssetOrder o : bids) {
            				JsonArray bidJson = new JsonArray();
            				// array with price and quantity
            				bidJson.add(o.getPrice().doubleValue());
            				bidJson.add(o.getQuantity().doubleValue());
            				
            				bidsJson.add(bidJson);
            			}
            			retJson.addProperty("bids", bidsJson.toString());
            			retJson.addProperty("asks", asksJson.toString());
            		}
            		else {
            			ArrayList<ContractState> bidsArray = new ArrayList<>();
            			ArrayList<ContractState> asksArray = new ArrayList<>();
            			
            			for(ContractState s: Contracts.getContracts()) {
            				if(s.getMarket() != m.getID() || s.getState() != SellContract.STATE_OPEN)
            					continue;
            				if(s.getType() == ContractType.SELL)
            					asksArray.add(s);
            				else
            					bidsArray.add(s);
            			}
            			
            			// sort them out
            			bidsArray.sort(new Comparator<ContractState>() {
            				@Override
            				public int compare(ContractState t1, ContractState t2) {
            					return (int)(t2.getRate() - t1.getRate());
            				}
            			});
            			asksArray.sort(new Comparator<ContractState>() {
            				@Override
            				public int compare(ContractState t1, ContractState t2) {
            					return (int)(t1.getRate() - t2.getRate());
            				}
            			});
            			
            			for (ContractState s : asksArray) {
            				JsonArray offerJson = new JsonArray();
            				// array with price and quantity
            				offerJson.add(s.getRate()/1e8);
            				offerJson.add(s.getAmountNQT()/1e8);
            				
           					asksJson.add(offerJson);							
						}
            			for (ContractState s : bidsArray) {
            				JsonArray offerJson = new JsonArray();
            				// array with price and quantity
            				offerJson.add(s.getRate()/1e8);
            				offerJson.add(s.getAmountNQT()/1e8);
            				
           					bidsJson.add(offerJson);							
						}
            		}
        			retJson.addProperty("bids", bidsJson.toString());
        			retJson.addProperty("asks", asksJson.toString());
            		
                    ret = retJson.toString();
                    break;
            	}
            }
        }
        else if (uri.startsWith(API_PREFIX + "trades/")) {
        	
            for(Market m : Markets.getMarkets()) {
            	String pair = m.getTokenID() != null ? Constants.BURST_TICKER + "_" + m.toString() : m.toString() + "_" + Constants.BURST_TICKER;

            	if(uri.endsWith(pair)) {
            		JsonArray retJson = new JsonArray();
                    
            		if(m.getTokenID() != null) {
            			AssetTrade[] trades = bn.getAssetTrades(m);
                        
            			for(AssetTrade t : trades) {
            				JsonObject tradeJson = new JsonObject();
            				
            				tradeJson.addProperty("trade_id", t.getBidOrderId().getID() + "_" + t.getAskOrderId().getID());
            				tradeJson.addProperty("price", t.getPrice().multiply(m.getFactor()).doubleValue());
            				
            				tradeJson.addProperty("base_volume", t.getQuantity().multiply(m.getFactor()).doubleValue());
            				tradeJson.addProperty("quote_volume", t.getQuantity().multiply(t.getPrice().longValue()).doubleValue());
            				tradeJson.addProperty("timestamp", t.getTimestamp().getAsDate().getTime());
            				tradeJson.addProperty("type", t.getType() == TradeType.SELL ? "sell" : "buy");
            				
            				retJson.add(tradeJson);
            			}
            		}
            		else {
            			for(ContractTrade t: Contracts.getTrades()) {
            				if(m.getID() != t.getMarket())
            					continue;
            				
            				JsonObject tradeJson = new JsonObject();
            				tradeJson.addProperty("trade_id", t.getTakeID().getID());
            				tradeJson.addProperty("price", t.getRate()/1e8);
            				tradeJson.addProperty("base_volume", t.getAmount()/1e8);
            				tradeJson.addProperty("quote_volume", t.getRate()/1e8 * t.getAmount()/1e8);
            				tradeJson.addProperty("timestamp", t.getTimestamp().getAsDate().getTime());
            				tradeJson.addProperty("type", t.getContract().getType() == ContractType.SELL ? "sell" : "buy");
            				
            				retJson.add(tradeJson);
            			}
            		}
                    ret = retJson.toString();
                    break;
            	}
            }
        }
        
        return ret;
    }
}
