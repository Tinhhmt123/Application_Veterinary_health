package com.example.healthy_pet.repository;

import com.example.healthy_pet.models.Appointment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository dùng chung lưu trữ & đồng bộ danh sách Lịch hẹn (Appointments)
 * giữa Luồng USER (NV 5b - Đặt lịch) và Luồng ADMIN (NV 7 - Duyệt lịch & Kê đơn).
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
        Appointment apt1 = new Appointment(1L, 101L, 1L, "Khám bệnh", "2026-10-25 09:30:00", new BigDecimal("250000"), "Khám da liễu & tiêm phòng");
        apt1.setId(501L);
        apt1.setPetName("LuLu (Poodle)");
        apt1.setClinicName("Healthy Pet Q.1");
        apt1.setStatus("PENDING");

        Appointment apt2 = new Appointment(1L, 102L, 2L, "Spa & Làm đẹp", "2026-10-20 14:00:00", new BigDecimal("350000"), "Tắm sấy & cắt tỉa lông");
        apt2.setId(502L);
        apt2.setPetName("Miu Miu (Mèo Anh)");
        apt2.setClinicName("Healthy Pet Bình Thạnh");
        apt2.setStatus("CONFIRMED");

        Appointment apt3 = new Appointment(1L, 101L, 1L, "Khám bệnh", "2026-10-18 10:00:00", new BigDecimal("350000"), "Khám viêm tai ngoài");
        apt3.setId(503L);
        apt3.setPetName("LuLu (Poodle)");
        apt3.setClinicName("Healthy Pet Q.1");
        apt3.setStatus("COMPLETED");

        appointmentList.add(apt1);
        appointmentList.add(apt2);
        appointmentList.add(apt3);
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
