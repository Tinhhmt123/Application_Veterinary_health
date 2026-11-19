package com.example.healthy_pet.repository;

import com.example.healthy_pet.models.Appointment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository dùng chung lưu trữ & đồng bộ danh sách Lịch hẹn (Appointments)
 * giữa Luồng USER (NV 5b - Đặt lịch) và Luồng ADMIN (NV 7 - Duyệt lịch & Kê đơn).
 * Khớp 100% với Seed Data SQL (CreateTableAndInsertDataExample.sql).
 */
public class AppointmentRepository {
    private static AppointmentRepository instance;
    private final List<Appointment> appointmentList = new ArrayList<>();

    private AppointmentRepository() {
        initSampleAppointments();
    }

    public static synchronized AppointmentRepository getInstance() {
        if (instance == null) {
            instance = new AppointmentRepository();
        }
        return instance;
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
        apt1.setStatus("COMPLETED");

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
                    return;
                }
            }
            appointmentList.add(0, appointment);
        }
    }

    public synchronized void updateStatus(Long appointmentId, String newStatus) {
        if (appointmentId == null || newStatus == null) return;
        for (Appointment apt : appointmentList) {
            if (appointmentId.equals(apt.getId())) {
                apt.setStatus(newStatus);
                break;
            }
        }
    }

    public synchronized void setAppointments(List<Appointment> newList) {
        if (newList != null && !newList.isEmpty()) {
            this.appointmentList.clear();
            this.appointmentList.addAll(newList);
        }
    }
}
