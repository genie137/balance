package com.avermak.vkube.balance;

import javafx.util.StringConverter;

import java.text.NumberFormat;

public class TimeAxisLabelConverter extends StringConverter<Number> {
    @Override
    public String toString(Number n) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(2);
        long val = (long) n.doubleValue();
        val /= 1000;
        if (val < 3600) {
            return nf.format(val/60) + ":" + nf.format(val%60);
        } else {
            return val/3600 + ":" + nf.format((val%3600)/60) + ":" + nf.format((val%3600)%60);
        }
    }

    @Override
    public Number fromString(String string) {
        return 0;
    }
}

