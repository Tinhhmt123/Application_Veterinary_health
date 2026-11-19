package com.example.healthy_pet.repository;

import com.example.healthy_pet.models.MedicalHistory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository dùng chung lưu trữ & đồng bộ danh sách Bệnh án (MedicalHistories)
 * giữa Luồng ADMIN (NV 7 - Kê đơn bệnh án) và Luồng USER (NV 4 - Xem bệnh án & Thống kê chi phí).
 * Khớp 100% với Seed Data SQL (CreateTableAndInsertDataExample.sql).
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
                1L, 1L, 1L, "2026-10-18",
                "Ngứa tai, lắc đầu liên tục",
                "Viêm ống tai ngoài do nấm Otodectes",
                "Thuốc nhỏ tai Oridermyl 10g (nhỏ 2 lần/ngày trong 7 ngày)",
                new BigDecimal("350000"),
                null,
                "UNPAID",
                null
        );
        h1.setPetName("Milu (Golden)");
        h1.setUserId(2L);

        MedicalHistory h2 = new MedicalHistory(
                2L, null, 2L, "2026-09-25",
                "Khám định kỳ & tẩy giun",
                "Sức khỏe bình thường",
                "Viên tẩy giun Drontal Cat (1 viên)",
                new BigDecimal("120000"),
                null,
                "PAID",
                "CASH"
        );
        h2.setPetName("Mochi (Mèo Anh)");
        h2.setUserId(2L);

        historyList.add(h1);
        historyList.add(h2);
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
