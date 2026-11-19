package com.example.healthy_pet.utils.payment;

import android.content.Context;
import android.view.View;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.models.MedicalHistory;

/**
 * [Strategy Pattern] Interface định nghĩa chiến lược thanh toán viện phí (NV 4).
 * Mỗi Concrete Strategy tự quản lý logic nghiệp vụ VÀ giao diện preview riêng.
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
     * Xây dựng View preview cho Dialog xác nhận thanh toán.
     * Mỗi Strategy tự quyết định UI riêng (QR code, hướng dẫn tiền mặt, ...)
     * giúp Fragment không cần if/else kiểm tra methodCode.
     */
    View buildPreviewView(Fragment fragment, MedicalHistory medicalHistory);

    /**
     * Thực thi xử lý thanh toán và cập nhật trạng thái cho bản ghi MedicalHistory
     */
    String executePayment(Context context, MedicalHistory medicalHistory);
}
