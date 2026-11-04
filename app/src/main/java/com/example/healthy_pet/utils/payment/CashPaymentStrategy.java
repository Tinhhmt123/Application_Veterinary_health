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
        return "Tiền mặt tại quầy thu ngân (CASH)";
    }

    @Override
    public String buildPaymentInfoOrQrUrl(MedicalHistory medicalHistory) {
        return "Vui lòng đọc mã bệnh án #" + medicalHistory.getId()
                + " và thanh toán " + CurrencyUtils.formatVND(medicalHistory.getCost())
                + " trực tiếp tại quầy lễ tân phòng khám.";
    }

    @Override
    public String executePayment(Context context, MedicalHistory medicalHistory) {
        medicalHistory.setPaymentStatus("PAID");
        medicalHistory.setPaymentMethod(getMethodCode());
        return "Đã xác nhận thanh toán tiền mặt (" + CurrencyUtils.formatVND(medicalHistory.getCost()) + ") tại quầy!";
    }
}
