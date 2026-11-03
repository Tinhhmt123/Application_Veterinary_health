package com.example.healthy_pet.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Định dạng tiền tệ chuẩn DECIMAL(18, 0) sang VNĐ (NV 4 & NV 7).
 */
public class CurrencyUtils {
    public static String formatVND(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + " đ";
    }
}
