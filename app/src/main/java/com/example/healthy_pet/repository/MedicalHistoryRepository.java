package com.example.healthy_pet.repository;

import com.example.healthy_pet.models.MedicalHistory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository dùng chung lưu trữ & đồng bộ danh sách Bệnh án (MedicalHistories)
 * giữa Luồng ADMIN (NV 7 - Kê đơn bệnh án) và Luồng USER (NV 4 - Xem bệnh án & Thống kê chi phí).
 */
public class MedicalHistoryRepository {
    private static MedicalHistoryRepository instance;
    private final List<MedicalHistory> historyList = new ArrayList<>();

    private MedicalHistoryRepository() {
        initSampleHistories();
    }

    public static synchronized MedicalHistoryRepository getInstance() {
        if (instance == null) {
            instance = new MedicalHistoryRepository();
        }
        return instance;
    }

    private void initSampleHistories() {
        MedicalHistory h1 = new MedicalHistory(
                101L, 503L, 101L, "2026-10-18",
                "Ngứa tai, lắc đầu liên tục",
                "Viêm ống tai ngoài do nấm Otodectes",
                "Thuốc nhỏ tai Oridermyl 10g (2 lần/ngày)",
                new BigDecimal("350000"),
                "https://res.cloudinary.com/demo/image/upload/prescription_101.jpg",
                "UNPAID",
                ""
        );
        h1.setPetName("LuLu (Poodle)");
        h1.setUserId(1L);

        MedicalHistory h2 = new MedicalHistory(
                102L, 502L, 102L, "2026-10-12",
                "Biếng ăn, nôn dịch vàng, sốt nhẹ",
                "Viêm đường tiêu hóa do vi khuẩn",
                "Thuốc Smecta 3g + Men vi sinh Bio-Flora + Điện giải",
                new BigDecimal("450000"),
                "https://res.cloudinary.com/demo/image/upload/prescription_102.jpg",
                "PAID",
                "VIETQR"
        );
        h2.setPetName("Miu Miu (Mèo Anh)");
        h2.setUserId(1L);

        MedicalHistory h3 = new MedicalHistory(
                103L, 501L, 103L, "2026-10-05",
                "Tiêm ngừa định kỳ & kiểm tra da",
                "Thú cưng hoàn toàn khỏe mạnh",
                "Vắc xin Rabies 1ml + Vitamin tổng hợp",
                new BigDecimal("450000"),
                "https://res.cloudinary.com/demo/image/upload/prescription_103.jpg",
                "PAID",
                "CASH"
        );
        h3.setPetName("Kiki (Corgi)");
        h3.setUserId(1L);

        historyList.add(h1);
        historyList.add(h2);
        historyList.add(h3);
    }

    public synchronized List<MedicalHistory> getMedicalHistoriesByUser(Long userId) {
        List<MedicalHistory> result = new ArrayList<>();
        for (MedicalHistory h : historyList) {
            if (userId == null || userId.equals(h.getUserId()) || h.getUserId() == null) {
                result.add(h);
            }
        }
        return result;
    }

    public synchronized void addMedicalHistory(MedicalHistory history) {
        if (history != null) {
            historyList.add(0, history);
        }
    }

    public synchronized void payMedicalHistory(Long historyId, String paymentMethod) {
        if (historyId == null) return;
        for (MedicalHistory h : historyList) {
            if (historyId.equals(h.getId())) {
                h.setPaymentStatus("PAID");
                h.setPaymentMethod(paymentMethod != null ? paymentMethod : "VIETQR");
                break;
            }
        }
    }

    public synchronized BigDecimal getTotalCostByUser(Long userId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (MedicalHistory h : historyList) {
            if (userId == null || userId.equals(h.getUserId()) || h.getUserId() == null) {
                if (h.getCost() != null) {
                    sum = sum.add(h.getCost());
                }
            }
        }
        return sum;
    }

    public synchronized void setHistories(List<MedicalHistory> newList) {
        if (newList != null && !newList.isEmpty()) {
            this.historyList.clear();
            this.historyList.addAll(newList);
        }
    }
}
