package btdex.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import bt.Contract;

public class NumberFormatting {
	
	private NumberFormat nf;
	
	private NumberFormatting(NumberFormat nf) {
		this.nf = nf;
	}
	
    //NF_FULL min 5, max 8
    private static NumberFormatting NF(int minimumFractionDigits, int maximumFractionDigits){
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMinimumFractionDigits(minimumFractionDigits);
        nf.setMaximumFractionDigits(maximumFractionDigits);
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
        s.setGroupingSeparator('\'');
        ((DecimalFormat)nf).setDecimalFormatSymbols(s);
        return new NumberFormatting(nf);
    }
    
    public NumberFormat getFormat() {
    	return nf;
    }
    
	public String format(long valueNQT) {
		double dvalue = (double)valueNQT / Contract.ONE_BURST;
		return nf.format(dvalue);
	}
    
	public String format(double dvalue) {
		return nf.format(dvalue);
	}
    
    // Always all decimal places
    public static final NumberFormatting FULL = NF(8, 8);
    
    // Minimum of 4 decimal places
    public static final NumberFormatting FIAT = NF(4, 8);
    
    // Minimum of 5 decimal places
    public static final NumberFormatting BURST = NF(2, 8);

    // Minimum of 2 decimal places, max 4
    public static final NumberFormatting TOKEN = NF(2, 4);

    public static Number parse(String s) throws ParseException {
    	return FULL.nf.parse(s);
    }
}
