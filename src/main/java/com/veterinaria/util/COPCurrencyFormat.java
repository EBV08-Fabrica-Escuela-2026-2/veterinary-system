package com.veterinaria.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for formatting prices in Colombian Pesos (COP).
 * Format: $XX.XXX (no decimals, dot as thousands separator)
 * Example: $85.000, $120.500, $1.250.000
 */
public final class COPCurrencyFormat {

    private static final NumberFormat FORMAT = new DecimalFormat("$#,##0", new DecimalFormatSymbols(new Locale("es", "CO")));

    private COPCurrencyFormat() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Formats a BigDecimal price to COP format.
     * @param price The price to format
     * @return Formatted string like "$85.000"
     */
    public static String format(BigDecimal price) {
        if (price == null) {
            return "$0";
        }
        return FORMAT.format(price);
    }
}
