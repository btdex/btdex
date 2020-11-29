package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import btdex.core.Market;
import fr.marcwrobel.jbanking.iban.Iban;

public class MarketEUR extends Market {
	
	static final String METHOD = "method";
	static final String NAME = "owner";
	static final String IBAN = "iban";
	
	static final String SEPA = "SEPA";
	static final String SEPA_INSTANT = "SEPA Instant";

	public String getTicker() {
		return "EUR";
	}
	
	@Override
	public long getID() {
		return MARKET_EUR;
	}
	
	@Override
	public int getUCA_ID() {
		return 0;
	}
	
	@Override
	public ArrayList<String> getFieldKeys(){
		ArrayList<String> fieldNames = new ArrayList<>();
		fieldNames.add(METHOD);
		fieldNames.add(NAME);
		fieldNames.add(IBAN);
		return fieldNames;
	}
	
	@Override
	public String getFieldDescription(String key) {
		switch (key) {
		case METHOD:
			return "Payment method";
		case NAME:
			return "Account Owner Full Name";
		case IBAN:
			return "IBAN";
		default:
			break;
		}
		
		return super.getFieldDescription(key);
	}
	
	@Override
	public int getPaymentTimeout(HashMap<String, String> fields) {
		String method = fields.get(METHOD);
		switch (method) {
		case SEPA_INSTANT:
			return 24;
		default:
			break;
		}
		// 5 days to complete the payment
		return 5*24;
	}
	
	@Override
	public JComponent getFieldEditor(String key, boolean editable, HashMap<String, String> fields) {
		if(key.equals(METHOD)) {
			JComboBox<String> combo = new JComboBox<String>();
			
			combo.addItem(SEPA);
			combo.addItem(SEPA_INSTANT);
			
			combo.setSelectedIndex(0);
			combo.setEnabled(editable);
			
			String value = fields.get(key);
			if(value.equals(SEPA_INSTANT))
				combo.setSelectedIndex(1);
			
			return combo;
		}
		return super.getFieldEditor(key, editable, fields);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void setFieldValue(String key, JComponent editor, HashMap<String, String> fields) {
		if(key.equals(METHOD)) {
			JComboBox<String> combo = (JComboBox<String>) editor;
			fields.put(key, combo.getSelectedIndex() == 0 ? SEPA : SEPA_INSTANT);
		}
		else
			super.setFieldValue(key, editor, fields);
	}
	
	@Override
	public void validate(HashMap<String, String> fields) throws Exception {
		// All fields are required
		Set<String> keys = fields.keySet();
		for(String key : keys) {
			String f = fields.get(key);
			if(f == null || f.isEmpty())
				throw new Exception(key + " cannot be empty");
		}
		
		String iban = fields.get(IBAN);
		if(!Iban.isValid(iban))
			throw new Exception(iban + " is not a valid IBAN");
	}

	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		return fields.get(METHOD) + ":" + fields.get(IBAN);
	}
}
