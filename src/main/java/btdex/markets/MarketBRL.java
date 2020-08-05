package btdex.markets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import btdex.core.Market;

public class MarketBRL extends Market {

	static final String METHOD = "method";
	static final String NAME = "owner";
	static final String CPF = "cpf";
	static final String BANK = "bank";
	static final String AGENCY = "agency";
	static final String ACCOUNT = "account";

	static final String METHOD_SAME_BANK = "Same bank";
	static final String METHOD_TED = "TED";
	
	static final String BANK_LIST[] = {
			"001 - Banco do Brasil", 
			"341 - Itaú Unibanco",
			"237 - Bradesco",
			"033 - Santander Brasil", 
			"104 - Caixa Econômica Federal", 
			"756 - Bancoob - Sicoob", 
			"422 - Banco Safra", 
			"041 - Banrisul", 
			"004 - Banco do Nordeste",
			"077 - Banco Inter",
			"260 - NuBank",
	};
	
	@Override
	public String getTicker() {
		return "BRL";
	}

	@Override
	public long getID() {
		return MARKET_BRL;
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
		fieldNames.add(CPF);
		fieldNames.add(BANK);
		fieldNames.add(AGENCY);
		fieldNames.add(ACCOUNT);
		return fieldNames;
	}

	@Override
	public String getFieldDescription(String key) {
		switch (key) {
		case METHOD:
			return "Payment method";
		case NAME:
			return "Account Owner Full Name";
		case CPF:
			return "CPF";
		case BANK:
			return "Bank";
		case AGENCY:
			return "Agency";
		case ACCOUNT:
			return "Account";
		default:
			break;
		}

		return super.getFieldDescription(key);
	}

	@Override
	public int getPaymentTimeout(HashMap<String, String> fields) {
		String method = fields.get(METHOD);
		switch (method) {
		case METHOD_SAME_BANK:
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

			combo.addItem(METHOD_SAME_BANK);
			combo.addItem(METHOD_TED);

			combo.setSelectedIndex(0);
			combo.setEnabled(editable);

			String value = fields.get(key);
			if(value.equals(METHOD_TED))
				combo.setSelectedIndex(1);

			return combo;
		}
		if(key.equals(BANK)) {
			JComboBox<String> combo = new JComboBox<String>();
			String value = fields.get(key);

			int i = 0;
			for (String bank : BANK_LIST) {
				combo.addItem(bank);
				if(value!=null && bank.startsWith(value))
					combo.setSelectedIndex(i);
				i++;
			}
			combo.setEnabled(editable);

			return combo;
		}
		return super.getFieldEditor(key, editable, fields);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setFieldValue(String key, JComponent editor, HashMap<String, String> fields) {
		if(key.equals(METHOD)) {
			JComboBox<String> combo = (JComboBox<String>) editor;
			fields.put(key, combo.getSelectedIndex() == 0 ? METHOD_SAME_BANK : METHOD_TED);
		}
		else if(key.equals(BANK)) {
			JComboBox<String> combo = (JComboBox<String>) editor;
			// save only the bank number
			fields.put(key, combo.getSelectedItem().toString().substring(0, 3));
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

		String cpf = fields.get(CPF);
		if(!isCPF(cpf))
			throw new Exception(cpf + " is not a valid CPF");
	}

	public static boolean isCPF(String CPF) {
		// considera-se erro CPF's formados por uma sequencia de numeros iguais
		if (CPF.equals("00000000000") ||
				CPF.equals("11111111111") ||
				CPF.equals("22222222222") || CPF.equals("33333333333") ||
				CPF.equals("44444444444") || CPF.equals("55555555555") ||
				CPF.equals("66666666666") || CPF.equals("77777777777") ||
				CPF.equals("88888888888") || CPF.equals("99999999999") ||
				(CPF.length() != 11))
			return(false);

		char dig10, dig11;
		int sm, i, r, num, peso;

		// "try" - protege o codigo para eventuais erros de conversao de tipo (int)
		try {
			// Calculo do 1o. Digito Verificador
			sm = 0;
			peso = 10;
			for (i=0; i<9; i++) {              
				// converte o i-esimo caractere do CPF em um numero:
				// por exemplo, transforma o caractere '0' no inteiro 0         
				// (48 eh a posicao de '0' na tabela ASCII)         
				num = (int)(CPF.charAt(i) - 48); 
				sm = sm + (num * peso);
				peso = peso - 1;
			}

			r = 11 - (sm % 11);
			if ((r == 10) || (r == 11))
				dig10 = '0';
			else dig10 = (char)(r + 48); // converte no respectivo caractere numerico

			// Calculo do 2o. Digito Verificador
			sm = 0;
			peso = 11;
			for(i=0; i<10; i++) {
				num = (int)(CPF.charAt(i) - 48);
				sm = sm + (num * peso);
				peso = peso - 1;
			}

			r = 11 - (sm % 11);
			if ((r == 10) || (r == 11))
				dig11 = '0';
			else dig11 = (char)(r + 48);

			// Verifica se os digitos calculados conferem com os digitos informados.
			if ((dig10 == CPF.charAt(9)) && (dig11 == CPF.charAt(10)))
				return(true);
			else return(false);
		} catch (InputMismatchException erro) {
			return(false);
		}
	}

	@Override
	public String simpleFormat(HashMap<String, String> fields) {
		String bank = null;
		for (int i = 0; i < BANK_LIST.length; i++) {
			if(BANK_LIST[i].startsWith(fields.get(BANK)))
				bank = BANK_LIST[i];
		}
		return fields.get(METHOD) + ":" + bank + ":" + fields.get(ACCOUNT);
	}
}
