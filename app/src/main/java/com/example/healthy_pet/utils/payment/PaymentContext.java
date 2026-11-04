package com.example.healthy_pet.utils.payment;

import android.content.Context;
import com.example.healthy_pet.models.MedicalHistory;

/**
 * [Strategy Context] Lớp ngữ cảnh điều phối chiến lược thanh toán lúc Runtime.
 * Giúp tách biệt hoàn toàn logic thanh toán khỏi Fragment/Activity.
 */
public class PaymentContext {

    private PaymentStrategy strategy;

    public PaymentContext() {
        // Mặc định ưu tiên VietQR
        this.strategy = new VietQrPaymentStrategy();
    }

    public PaymentContext(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public PaymentStrategy getStrategy() {
        return strategy;
    }

    public String getPreviewInfo(MedicalHistory medicalHistory) {
        if (strategy == null) {
            throw new IllegalStateException("Chưa chọn phương thức thanh toán (PaymentStrategy)!");
        }
        return strategy.buildPaymentInfoOrQrUrl(medicalHistory);
    }

    public String processPayment(Context context, MedicalHistory medicalHistory) {
        if (strategy == null) {
            throw new IllegalStateException("Chưa chọn phương thức thanh toán (PaymentStrategy)!");
        }
        return strategy.executePayment(context, medicalHistory);
    }
}
