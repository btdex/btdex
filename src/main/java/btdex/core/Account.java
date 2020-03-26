package btdex.core;

import java.util.HashMap;

public class Account {
	private String market;
	private	String name;
	private HashMap<String, String> fields;
	
	public Account(String market, String name, HashMap<String, String> fields) {
		this.market = market;
		this.name = name;
		this.fields = fields;
	}
	
	@Override
	public String toString() {
		return name;
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
