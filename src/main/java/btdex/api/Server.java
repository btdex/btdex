package btdex.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Market;
import btdex.core.Markets;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.AssetTrade;
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
                    
            		if(m.getTokenID() != null) {
            			AssetOrder[] asks = bn.getAssetAsks(m);
                        JsonArray asksJson = new JsonArray();
            			for(AssetOrder o : asks) {
            				JsonArray askJson = new JsonArray();
            				// array with price and quantity
            				askJson.add(o.getPrice().doubleValue());
            				askJson.add(o.getQuantity().doubleValue());
            				
            				asksJson.add(askJson);
            			}
            			retJson.addProperty("asks", asksJson.toString());
            			
            			AssetOrder[] bids = bn.getAssetBids(m);
                        JsonArray bidsJson = new JsonArray();
            			for(AssetOrder o : bids) {
            				JsonArray bidJson = new JsonArray();
            				// array with price and quantity
            				bidJson.add(o.getPrice().doubleValue());
            				bidJson.add(o.getQuantity().doubleValue());
            				
            				bidsJson.add(bidJson);
            			}
            			retJson.addProperty("bids", bidsJson.toString());
            		}
            		
                    ret = retJson.toString();
                    break;
            	}
            }
        }
        else if (uri.startsWith(API_PREFIX + "trades/")) {
        	
        	// one for every market pair
        	if(uri.endsWith("BURST_BTC")) {
        		
        	}
        	else if(uri.endsWith("BURST_LTC")) {
        		
        	}
        	
//        	{         
//        	      "trade_id":3523643,
//        	      "price":"0.01",
//        	      "base_volume":"569000",
//        	      "quote_volume":"0.01000000",
//        	      "timestamp":"‭1585177482652‬",
//        	      "type":"sell"
//        	   }

        }
        
        return ret;
    }
}
