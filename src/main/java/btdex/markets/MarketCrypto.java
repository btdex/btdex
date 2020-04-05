package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;

import btdex.core.Account;
import btdex.core.Market;

/**
 * Basic market for cryptocurrencies.
 * 
 * @author jjos
 *
 */
public abstract class MarketCrypto extends Market {
	
	static final String ADDRESS = "address";

	public String toString() {
		return "BTC";
	}
	
	@Override
	public ArrayList<String> getFieldKeys(){
		ArrayList<String> fieldNames = new ArrayList<>();
		fieldNames.add(ADDRESS);
		return fieldNames;
	}
	
	@Override
	public String getFieldDescription(String key) {
		return "Address";
	}
	
	@Override
	public int getPaymentTimeout(HashMap<String, String> fields) {
		return 2;
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		String addr = fields.get(ADDRESS);
		
		if(addr == null || addr.isEmpty())
			throw new Exception("Address cannot be empty");
	}

	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		return fields.get(ADDRESS);
	}
	
	@Override
	public Account parseAccount(String accountFields) {
		String fieldsArray[] = accountFields.split(":");
		
		if(fieldsArray.length!=1)
			return null;
		
		HashMap<String, String> fields = new HashMap<>();
		fields.put(ADDRESS, fieldsArray[0]);
		try {
			validate(fields);
		}
		catch (Exception e) {
			return null;
		}
		Account ret = new Account(this.toString(), null, fields);
		return ret;
	}
}
