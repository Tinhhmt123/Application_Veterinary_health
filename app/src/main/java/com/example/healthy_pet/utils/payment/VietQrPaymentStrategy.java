package com.example.healthy_pet.utils.payment;

import android.content.Context;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.utils.CurrencyUtils;
import java.math.BigDecimal;

/**
 * [Concrete Strategy 1] Chiến lược thanh toán chuyển khoản Ngân hàng qua mã VietQR động.
 * Tự động tạo link ảnh QR từ img.vietqr.io khớp với số tiền cost DECIMAL(18, 0) để tải bằng Glide.
 */
public class VietQrPaymentStrategy implements PaymentStrategy {

    private static final String BANK_ID = "VCB";
    private static final String ACCOUNT_NO = "1031183832";
    private static final String ACCOUNT_NAME = "NGUYEN TRONG TINH";

    @Override
    public String getMethodCode() {
        return "VIETQR";
    }

    @Override
    public String getDisplayName() {
        return "Chuyển khoản Ngân hàng (VietQR)";
    }

    @Override
    public String buildPaymentInfoOrQrUrl(MedicalHistory medicalHistory) {
        BigDecimal cost = medicalHistory.getCost() != null ? medicalHistory.getCost() : BigDecimal.ZERO;
        Long id = medicalHistory.getId() != null ? medicalHistory.getId() : 101L;
        String addInfo = "HPET" + id;

        return "https://img.vietqr.io/image/" + BANK_ID + "-" + ACCOUNT_NO
                + "-compact2.png?amount=" + cost.toPlainString()
                + "&addInfo=" + addInfo
                + "&accountName=" + ACCOUNT_NAME;
    }

    @Override
    public String executePayment(Context context, MedicalHistory medicalHistory) {
        medicalHistory.setPaymentStatus("PAID");
        medicalHistory.setPaymentMethod(getMethodCode());
        return "Đã thanh toán " + CurrencyUtils.formatVND(medicalHistory.getCost())
                + " qua mã VietQR (Nội dung: HPET" + medicalHistory.getId() + ")";
    }
}
