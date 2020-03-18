package btdex.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormatting {

    public static NumberFormat NF(){
        NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
        NF.setMinimumFractionDigits(2);
        NF.setMaximumFractionDigits(5);
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
        s.setGroupingSeparator('\'');
        ((DecimalFormat)NF).setDecimalFormatSymbols(s);
        return NF;
    }

    public static NumberFormat NF_FULL(){
        NumberFormat NF_FULL = NumberFormat.getInstance(Locale.ENGLISH);
        NF_FULL.setMinimumFractionDigits(5);
        NF_FULL.setMaximumFractionDigits(8);
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
        s.setGroupingSeparator('\'');
        ((DecimalFormat)NF_FULL).setDecimalFormatSymbols(s);
        return NF_FULL;
    }
    //TODO test these formats








}
