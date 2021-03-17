package btdex.core;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JTextField;

import com.google.gson.JsonObject;

import burst.kit.entity.BurstID;


public abstract class Market {

	public static long BTC_TO_SAT = 100000000L;
	public static long BURST_TO_PLANCK = 100000000L;
	
	// Unified crypto asset ID https://pro-api.coinmarketcap.com/v1/cryptocurrency/map?CMC_PRO_API_KEY=UNIFIED-CRYPTOASSET-INDEX&listing_status=active
	public static int UCA_ID_BURST = 573;

	public static long MARKET_BTC            = 0x000000001;
	public static long MARKET_LTC            = 0x000000002;
	public static long MARKET_ETH            = 0x000000003;
	public static long MARKET_XMR            = 0x000000004;
	public static long MARKET_DOGE           = 0x000000005;
	public static long MARKET_ARRR           = 0x000000006;
	public static long MARKET_XLA            = 0x000000007;
	public static long MARKET_BNB            = 0x000000008;

	// TODO: fill with other cryptos here

	public static long MARKET_USD            = 0x000001000;
	public static long MARKET_EUR            = 0x000002000;
	public static long MARKET_BRL            = 0x000003000;
	// TODO: fill with other fiat currencies here

	public static long MARKET_MASK           = 0x0000fffff;

	public static long TRANSFER_SAME_BANK    = 0x000100000;
	public static long TRANSFER_NATIONAL_BANK= 0x000200000;
	public static long TRANSFER_SEPA         = 0x000300000;
	public static long TRANSFER_SEPA_INST    = 0x000400000;
	public static long TRANSFER_ZELLE        = 0x000500000;
	// TODO: fill with other FIAT transfer methods
	public static long TRANSFER_MASK         = 0x00ff00000;


	/**
	 * If this market is for a BURST token, should return its ID.
	 *
	 * @return the BURST token ID or null if not a token.
	 */
	public BurstID getTokenID() {
		return null;
	}

	/**
	 * @return the ticker for this market, e.g. BTC, BURST.
	 */
	public abstract String getTicker();

	/**
	 * @return true if this market is for a conventional fiat currency.
	 */
	public boolean isFiat() {
		return getTokenID()==null && getID() >= MARKET_USD;
	}

	/**
	 * @return a unique ID for the market or 0 if is a BURST token, see {@link #getTokenID()}.
	 */
	public abstract long getID();

	/**
	 * @return the UCA_ID https://pro-api.coinmarketcap.com/v1/cryptocurrency/map?CMC_PRO_API_KEY=UNIFIED-CRYPTOASSET-INDEX&listing_status=active
	 */
	public abstract int getUCA_ID();

	/**
	 * @return the formatted value (assuming value is in SATs)
	 */
	public String format(long value) {
		double dvalue = (double)value/BTC_TO_SAT;
		return getNumberFormat().format(dvalue);
	}

	public NumberFormatting getNumberFormat() {
		return NumberFormatting.FULL;
	}

	/**
	 * Factor to get amounts as a long (maximum number of decimal places).
	 *
	 * @return the multiplying factor to get amounts on this market as a long
	 */
	public long getFactor() {
		return BTC_TO_SAT;
	}

	/**
	 * @return the number of hours a taker has to make the crypto/fiat payment.
	 */
	public int getPaymentTimeout(HashMap<String, String> fields) {
		return 0;
	}

	/**
	 * @return the expected field keys when selling on this market.
	 */
	public abstract ArrayList<String> getFieldKeys();

	/**
	 * @param key the field key
	 * @return the description for the given field key
	 */
	public String getFieldDescription(String key) {
		return key;
	}

	public JComponent getFieldEditor(String key, boolean editable, HashMap<String, String> fields) {
		JTextField textField = new JTextField(10);
		textField.setEditable(editable);

		textField.setText(fields.get(key));

		return textField;
	}

	public void setFieldValue(String key, JComponent editor, HashMap<String, String> fields) {
		if(editor instanceof JTextField) {
			fields.put(key, ((JTextField)editor).getText().trim());
		}
	}

	/**
	 * Should parse the given account fields for this market (not needed for tokens).
	 *
	 * @param accountFields
	 * @return the account for the given fields or null if invalid
	 */
	public MarketAccount parseAccount(String accountFields) {
		return null;
	}

	/**
	 * Should validate the given field values, throwing an exception if invalid
	 *
	 * @param fields
	 * @throws Exception
	 */
	public abstract void validate(HashMap<String, String> fields) throws Exception;

	/**
	 * @param fields
	 * @return a simple formatted version of the fields
	 */
	public abstract String simpleFormat(HashMap<String, String> fields);

	/**
	 * Format the fields in a Json format.
	 *
	 * @param fields
	 * @throws Exception
	 */
	public String format(HashMap<String, String> fields) {
		JsonObject params = new JsonObject();
		for (String key: fields.keySet()) {
			params.addProperty(key, fields.get(key));
		}
		return params.toString();
	}
}
