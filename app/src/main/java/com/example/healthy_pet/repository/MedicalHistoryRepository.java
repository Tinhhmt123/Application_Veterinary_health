package com.example.healthy_pet.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.healthy_pet.models.MedicalHistory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository dùng chung lưu trữ & đồng bộ danh sách Bệnh án (MedicalHistories)
 * giữa Luồng ADMIN (NV 7 - Kê đơn bệnh án) và Luồng USER (NV 4 - Xem bệnh án & Thống kê chi phí).
 * Hỗ trợ lưu trữ SharedPreferences + Gson để không bao giờ bị mất dữ liệu bệnh án/đơn thuốc.
 */
public class MedicalHistoryRepository {
    private static MedicalHistoryRepository instance;
    private final List<MedicalHistory> historyList = new ArrayList<>();
    private SharedPreferences prefs;
    private static final String PREF_NAME = "HealthyPetMedicalPrefs";
    private static final String KEY_HISTORIES = "histories_json";
    private final Gson gson = new Gson();

    private MedicalHistoryRepository() {
    }

    public static synchronized MedicalHistoryRepository getInstance() {
        if (instance == null) {
            instance = new MedicalHistoryRepository();
        }
        return instance;
    }

    public synchronized void initContext(Context context) {
        if (context != null) {
            if (prefs == null) {
                prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            }
            loadFromPrefs();
        }
    }

    private void loadFromPrefs() {
        if (prefs == null) return;
        if (prefs.contains(KEY_HISTORIES)) {
            String json = prefs.getString(KEY_HISTORIES, null);
            if (json != null && !json.trim().isEmpty() && !json.equals("[]")) {
                try {
                    Type type = new TypeToken<List<MedicalHistory>>(){}.getType();
                    List<MedicalHistory> saved = gson.fromJson(json, type);
                    if (saved != null && !saved.isEmpty()) {
                        for (MedicalHistory savedH : saved) {
                            if (savedH.getPrescriptionUrl() != null
                                    && savedH.getPrescriptionUrl().contains("res.cloudinary.com/demo/")) {
                                savedH.setPrescriptionImageUrl(null);
                            }
                            boolean found = false;
                            for (int i = 0; i < historyList.size(); i++) {
                                if (historyList.get(i).getId() != null && historyList.get(i).getId().equals(savedH.getId())) {
                                    historyList.set(i, savedH);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                historyList.add(savedH);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } else {
            saveToPrefs();
        }
    }

    private synchronized void saveToPrefs() {
        if (prefs != null) {
            try {
                String json = gson.toJson(historyList);
                prefs.edit().putString(KEY_HISTORIES, json).apply();
            } catch (Exception ignored) {
            }
        }
    }

    private void initSampleHistories() {
        MedicalHistory h1 = new MedicalHistory(
                1L, null, 1L, "2026-10-18",
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
            for (int i = 0; i < historyList.size(); i++) {
                MedicalHistory existing = historyList.get(i);
                boolean sameId = existing.getId() != null && existing.getId().equals(history.getId());
                boolean sameApt = existing.getAppointmentId() != null
                        && history.getAppointmentId() != null
                        && existing.getAppointmentId().equals(history.getAppointmentId());
                if (sameId || sameApt) {
                    historyList.set(i, history);
                    saveToPrefs();
                    return;
                }
            }
            historyList.add(0, history);
            saveToPrefs();
        }
    }

    public synchronized void payMedicalHistory(Long historyId, String paymentMethod) {
        if (historyId == null) return;
        for (MedicalHistory h : historyList) {
            if (historyId.equals(h.getId())) {
                h.setPaymentStatus("PAID");
                h.setPaymentMethod(paymentMethod != null ? paymentMethod : "VIETQR");
                saveToPrefs();
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

    public synchronized BigDecimal getUnpaidCostByUser(Long userId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (MedicalHistory h : historyList) {
            if (userId == null || userId.equals(h.getUserId()) || h.getUserId() == null) {
                String status = h.getPaymentStatus() != null ? h.getPaymentStatus().trim().toUpperCase() : "UNPAID";
                if (!"PAID".equals(status) && h.getCost() != null) {
                    sum = sum.add(h.getCost());
                }
            }
        }
        return sum;
    }

    public synchronized BigDecimal getPaidCostByUser(Long userId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (MedicalHistory h : historyList) {
            if (userId == null || userId.equals(h.getUserId()) || h.getUserId() == null) {
                String status = h.getPaymentStatus() != null ? h.getPaymentStatus().trim().toUpperCase() : "UNPAID";
                if ("PAID".equals(status) && h.getCost() != null) {
                    sum = sum.add(h.getCost());
                }
            }
        }
        return sum;
    }

    /**
     * Đồng bộ danh sách bệnh án của một User từ Server:
     * Xóa bỏ hoàn toàn các bản ghi tạm/cũ bị lệch của User đó và cập nhật đúng 1-1 với Database.
     */
    public synchronized void setHistoriesForUser(Long userId, List<MedicalHistory> serverList) {
        if (serverList == null) return;
        List<MedicalHistory> oldList = new ArrayList<>(historyList);

        for (MedicalHistory serverHistory : serverList) {
            if (serverHistory == null) continue;
            if (serverHistory.getUserId() == null && userId != null) {
                serverHistory.setUserId(userId);
            }
            if (serverHistory.getPrescriptionUrl() != null
                    && serverHistory.getPrescriptionUrl().contains("res.cloudinary.com/demo/")) {
                serverHistory.setPrescriptionImageUrl(null);
            }
            if (serverHistory.getPetName() == null || serverHistory.getPetName().trim().isEmpty()) {
                for (MedicalHistory old : oldList) {
                    if (old == null) continue;
                    boolean matchId = serverHistory.getId() != null && serverHistory.getId().equals(old.getId());
                    boolean matchApt = serverHistory.getAppointmentId() != null
                            && serverHistory.getAppointmentId().equals(old.getAppointmentId());
                    boolean matchPet = serverHistory.getPetId() != null
                            && serverHistory.getPetId().equals(old.getPetId());
                    if ((matchId || matchApt || matchPet)
                            && old.getPetName() != null && !old.getPetName().trim().isEmpty()) {
                        serverHistory.setPetName(old.getPetName());
                        break;
                    }
                }
            }
        }

        if (userId == null) {
            historyList.clear();
            historyList.addAll(serverList);
        } else {
            historyList.removeIf(h -> h == null
                    || h.getUserId() == null
                    || userId.equals(h.getUserId())
                    || containsMatchingHistory(serverList, h));
            historyList.addAll(serverList);
        }
        saveToPrefs();
    }

    private boolean containsMatchingHistory(List<MedicalHistory> list, MedicalHistory target) {
        if (list == null || target == null) return false;
        for (MedicalHistory item : list) {
            if (item == null) continue;
            if (item.getId() != null && item.getId().equals(target.getId())) return true;
            if (item.getAppointmentId() != null && item.getAppointmentId().equals(target.getAppointmentId())) return true;
        }
        return false;
    }

    public synchronized void setHistories(List<MedicalHistory> newList) {
        setHistoriesForUser(null, newList);
    }
}
