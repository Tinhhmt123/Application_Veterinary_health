package com.example.healthy_pet.utils.payment;

import android.content.Context;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.utils.CurrencyUtils;

/**
 * [Concrete Strategy 2] Chiến lược thanh toán bằng Tiền mặt trực tiếp tại quầy thu ngân.
 */
public class CashPaymentStrategy implements PaymentStrategy {

    @Override
    public String getMethodCode() {
        return "CASH";
    }

    @Override
    public String getDisplayName() {
        return "Tiền mặt trực tiếp tại quầy thu ngân (CASH)";
    }

    @Override
    public String buildPaymentInfoOrQrUrl(MedicalHistory medicalHistory) {
        if (medicalHistory == null) {
            return "Vui lòng thanh toán tiền mặt trực tiếp tại quầy lễ tân.";
        }
        return "Vui lòng đọc mã hóa đơn Bệnh án #" + medicalHistory.getId()
                + " và nộp " + CurrencyUtils.formatVND(medicalHistory.getCost())
                + " tiền mặt trực tiếp cho thu ngân tại phòng khám.";
    }

    @Override
    public String executePayment(Context context, MedicalHistory medicalHistory) {
        if (medicalHistory != null) {
            medicalHistory.setPaymentStatus("PAID");
            medicalHistory.setPaymentMethod(getMethodCode());
            return "Đã xác nhận thu thành công " + CurrencyUtils.formatVND(medicalHistory.getCost()) + " tiền mặt tại quầy!";
        }
        return "Đã hoàn thành thanh toán tiền mặt!";
    }
}

