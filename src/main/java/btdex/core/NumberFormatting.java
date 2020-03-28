package btdex.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class NumberFormatting {
    //NF_FULL min 5, max 8
    private static NumberFormat NF(int minimumFractionDigits, int maximumFractionDigits){
        NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
        NF.setMinimumFractionDigits(minimumFractionDigits);
        NF.setMaximumFractionDigits(maximumFractionDigits);
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
        s.setGroupingSeparator('\'');
        ((DecimalFormat)NF).setDecimalFormatSymbols(s);
        return NF;
    }
    
    // Always all decimal places
    public static final NumberFormat FULL = NF(8, 8);
    
    // Minimum of 4 decimal places
    public static final NumberFormat FIAT = NF(4, 8);
    
    // Minimum of 5 decimal places
    public static final NumberFormat BURST = NF(5, 8);

    // Minimum of 2 decimal places, max 4
    public static final NumberFormat TOKEN = NF(2, 4);

    public static Number parse(String s) throws ParseException {
    	return FULL.parse(s);
    }
}
