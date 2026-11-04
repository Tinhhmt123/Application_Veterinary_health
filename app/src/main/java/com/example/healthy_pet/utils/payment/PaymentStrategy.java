package com.example.healthy_pet.utils.payment;

import android.content.Context;
import com.example.healthy_pet.models.MedicalHistory;

/**
 * [Strategy Pattern] Interface định nghĩa chiến lược thanh toán viện phí (NV 4).
 */
public interface PaymentStrategy {

    /**
     * Mã phương thức lưu vào Database ("VIETQR" hoặc "CASH")
     */
    String getMethodCode();

    /**
     * Tên hiển thị trên giao diện Android
     */
    String getDisplayName();

    /**
     * Tạo đường dẫn ảnh mã QR (VietQR) hoặc hướng dẫn thanh toán theo số tiền DECIMAL(18, 0)
     */
    String buildPaymentInfoOrQrUrl(MedicalHistory medicalHistory);

    /**
     * Thực thi xử lý thanh toán và cập nhật trạng thái cho bản ghi MedicalHistory
     */
    String executePayment(Context context, MedicalHistory medicalHistory);
}
