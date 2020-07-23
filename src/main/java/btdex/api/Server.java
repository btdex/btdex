package btdex.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

import btdex.core.Globals;
import fi.iki.elonen.NanoHTTPD;

public class Server extends NanoHTTPD {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    
    private static final String API_PREFIX = "/api/v1/";

    public Server(int port) {
        super(port);
        logger.info("API server started at port {}", port);
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
    	Globals g = Globals.getInstance();
    	String ret = null;
        
        if (uri.startsWith(API_PREFIX + "summary")) {
            JsonArray pairsJson = new JsonArray();
            
//    json array...
//    "trading_pairs": "ETC_BTC",
//    "base_currency": "BTC",        
//    "quote_currency": "USD",
//    "last_price": 0.00067,
//    "lowest_ask": 0.000681,
//    "highest_bid": 0.00067,
//    "base_volume": 1528.11,
//    "quote_volume": 1.0282814600000003,
//    "price_change_percent_24h": -1.3254786450662739,
//    "highest_price_24h": 0.000676,
//    "lowest_price_24h": 0.000666
            
            return null;
            
        }
        else if (uri.startsWith(API_PREFIX + "ticker")) {
        	
//        	json array
//        	{  
//        		   "BTC_USDT":{  
//        		      "base_id":"1",
//        		      "quote_id":"825",
//        		      "last_price":"10000",
//        		      "quote_volume":"20000",
//        		      "base_volume":"2",   
//        		      "isFrozen":"0"
//        		   },
//				...

        }
        else if (uri.startsWith(API_PREFIX + "orderbook/")) {
        	
        	// one for every market pair
        	if(uri.endsWith("BURST_BTC")) {
        		
        	}
        	else if(uri.endsWith("BURST_LTC")) {
        		
        	}

//        	{  
//        		   "timestamp":"‭1585177482652‬",
//        		   "bids":[  
//        		      [  
//        		         "12462000",
//        		         "0.04548320"
//        		      ],
//        		      [  
//        		         "12457000",
//        		         "3.00000000"
//        		      ]
//        		   ],
//        		   "asks":[  
//        		      [  
//        		         "12506000",
//        		         "2.73042000"
//        		      ],
//        		      [  
//        		         "12508000",
//        		         "0.33660000"
//        		      ]
//        		   ]
//        		}


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
