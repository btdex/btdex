package btdex.core;

import java.util.HashMap;

public class Account {
	String market;
	String name;
	HashMap<String, String> fields;
	
	public Account(String market, String name, HashMap<String, String> fields) {
		this.market = market;
		this.name = name;
		this.fields = fields;
	}
	
	public String getMarket() {
		return market;
	}
	
	public String getName() {
		return name;
	}

	public HashMap<String, String> getFields(){
		return fields;
	}
}
