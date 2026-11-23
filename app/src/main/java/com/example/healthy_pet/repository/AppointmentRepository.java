package com.example.healthy_pet.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.healthy_pet.models.Appointment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository dùng chung lưu trữ & đồng bộ danh sách Lịch hẹn (Appointments)
 * giữa Luồng USER (NV 5b - Đặt lịch) và Luồng ADMIN (NV 7 - Duyệt lịch & Kê đơn).
 * Hỗ trợ lưu trữ SharedPreferences + Gson để không bao giờ bị mất dữ liệu ca khám.
 */
public class AppointmentRepository {
    private static AppointmentRepository instance;
    private final List<Appointment> appointmentList = new ArrayList<>();
    private SharedPreferences prefs;
    private static final String PREF_NAME = "HealthyPetAppointmentPrefs";
    private static final String KEY_APPOINTMENTS = "appointments_json";
    private final Gson gson = new Gson();

    private AppointmentRepository() {
        initSampleAppointments();
    }

    public static synchronized AppointmentRepository getInstance() {
        if (instance == null) {
            instance = new AppointmentRepository();
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
        if (prefs.contains(KEY_APPOINTMENTS)) {
            String json = prefs.getString(KEY_APPOINTMENTS, null);
            if (json != null && !json.trim().isEmpty() && !json.equals("[]")) {
                try {
                    Type type = new TypeToken<List<Appointment>>(){}.getType();
                    List<Appointment> saved = gson.fromJson(json, type);
                    if (saved != null && !saved.isEmpty()) {
                        for (Appointment savedApt : saved) {
                            boolean found = false;
                            for (int i = 0; i < appointmentList.size(); i++) {
                                if (appointmentList.get(i).getId() != null && appointmentList.get(i).getId().equals(savedApt.getId())) {
                                    appointmentList.set(i, savedApt);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                appointmentList.add(savedApt);
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
                String json = gson.toJson(appointmentList);
                prefs.edit().putString(KEY_APPOINTMENTS, json).apply();
            } catch (Exception ignored) {
            }
        }
    }

    private void initSampleAppointments() {
        Appointment apt1 = new Appointment(
                2L, 1L, 1L,
                "Khám chuyên khoa Tai - Da liễu",
                "2026-10-18 09:30:00",
                new BigDecimal("350000"),
                "Bé ngứa tai và lắc đầu nhiều"
        );
        apt1.setId(1L);
        apt1.setPetName("Milu (Golden)");
        apt1.setClinicName("Healthy Pet Quận 1");
        // FIX: ĐÃ DUYỆT (CONFIRMED) thay vì COMPLETED -> KHÔNG tự động kê đơn! Sẵn sàng để Bác sĩ/Admin bấm "Kê đơn & Hoàn thành"
        apt1.setStatus("CONFIRMED");

        Appointment apt2 = new Appointment(
                2L, 2L, 2L,
                "Tiêm nhắc Vaccine Phòng Dại",
                "2026-11-12 15:00:00",
                new BigDecimal("180000"),
                "Đặt lịch từ cảnh báo sổ tiêm"
        );
        apt2.setId(2L);
        apt2.setPetName("Mochi (Mèo Anh)");
        apt2.setClinicName("Healthy Pet Quận 3");
        // CHỜ DUYỆT (PENDING) -> Admin bấm "Duyệt lịch" hoặc "Từ chối"
        apt2.setStatus("PENDING");

        appointmentList.add(apt1);
        appointmentList.add(apt2);
    }

    public synchronized List<Appointment> getAllAppointments() {
        return new ArrayList<>(appointmentList);
    }

    public synchronized List<Appointment> getUserAppointments(Long userId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment apt : appointmentList) {
            if (userId == null || userId.equals(apt.getUserId()) || apt.getUserId() == null) {
                result.add(apt);
            }
        }
        return result;
    }

    public synchronized void addAppointment(Appointment appointment) {
        if (appointment != null) {
            for (int i = 0; i < appointmentList.size(); i++) {
                if (appointmentList.get(i).getId() != null && appointmentList.get(i).getId().equals(appointment.getId())) {
                    appointmentList.set(i, appointment);
                    saveToPrefs();
                    return;
                }
            }
            appointmentList.add(0, appointment);
            saveToPrefs();
        }
    }

    public synchronized void updateStatus(Long appointmentId, String newStatus) {
        if (appointmentId == null || newStatus == null) return;
        for (Appointment apt : appointmentList) {
            if (appointmentId.equals(apt.getId())) {
                apt.setStatus(newStatus);
                saveToPrefs();
                break;
            }
        }
    }

    /**
     * Đồng bộ danh sách lịch hẹn từ Server mà KHÔNG xóa mất dữ liệu cũ (Smart Merge).
     * Giữ nguyên petName, clinicName cục bộ nếu Server trả về null.
     */
    public synchronized void setAppointments(List<Appointment> newList) {
        if (newList == null || newList.isEmpty()) return;
        for (Appointment serverApt : newList) {
            boolean found = false;
            for (Appointment localApt : appointmentList) {
                if (serverApt.getId() != null && serverApt.getId().equals(localApt.getId())) {
                    if (serverApt.getStatus() != null) localApt.setStatus(serverApt.getStatus());
                    if (serverApt.getServiceType() != null) localApt.setServiceType(serverApt.getServiceType());
                    if (serverApt.getAppointmentTime() != null) localApt.setAppointmentTime(serverApt.getAppointmentTime());
                    if (serverApt.getEstimatedCost() != null) localApt.setEstimatedCost(serverApt.getEstimatedCost());
                    if (serverApt.getNote() != null) localApt.setNote(serverApt.getNote());
                    if (serverApt.getPetName() != null && !serverApt.getPetName().trim().isEmpty()) {
                        localApt.setPetName(serverApt.getPetName());
                    }
                    if (serverApt.getClinicName() != null && !serverApt.getClinicName().trim().isEmpty()) {
                        localApt.setClinicName(serverApt.getClinicName());
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                appointmentList.add(serverApt);
            }
        }
        saveToPrefs();
    }
}
