package com.example.healthy_pet.utils.payment;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.utils.CurrencyUtils;

/**
 * [Concrete Strategy 2] Chiến lược thanh toán bằng Tiền mặt trực tiếp tại quầy thu ngân.
 * Tự build View preview (hướng dẫn nộp tiền mặt) theo đúng Strategy Pattern.
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
    public View buildPreviewView(Fragment fragment, MedicalHistory medicalHistory) {
        Context context = fragment.requireContext();
        String previewInfo = buildPaymentInfoOrQrUrl(medicalHistory);
        String amountFormatted = CurrencyUtils.formatVND(medicalHistory.getCost());
        float density = context.getResources().getDisplayMetrics().density;
        int pad = (int) (18 * density);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        TextView tvCashGuide = new TextView(context);
        tvCashGuide.setText("💵 " + previewInfo + "\n\n• Mã hóa đơn: #HPET" + medicalHistory.getId()
                + "\n• Số tiền cần nộp: " + amountFormatted);
        tvCashGuide.setTextSize(14f);
        container.addView(tvCashGuide);

        return container;
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

