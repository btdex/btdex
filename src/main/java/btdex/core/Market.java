package btdex.core;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Market {
	
	public abstract long getID();
	
	/**
	 * @return the expected field names when selling on this market.
	 */
	public abstract ArrayList<String> getFieldNames();
	
	/**
	 * Should validate the given field values, throwing an exception if invalid
	 * 
	 * @param fields
	 * @throws Exception
	 */
	public abstract void validate(HashMap<String, String> fields) throws Exception;
}
