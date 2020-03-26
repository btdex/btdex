package btdex.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormatting {
    //NF_FULL min 5, max 8
    public static NumberFormat NF(int minimumFractionDigits, int maximumFractionDigits){
        NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
        NF.setMinimumFractionDigits(minimumFractionDigits);
        NF.setMaximumFractionDigits(maximumFractionDigits);
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
        s.setGroupingSeparator('\'');
        ((DecimalFormat)NF).setDecimalFormatSymbols(s);
        return NF;
    }









}
