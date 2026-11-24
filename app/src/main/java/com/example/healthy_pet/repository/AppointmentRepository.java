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

    public synchronized void removeAppointment(Long appointmentId) {
        if (appointmentId == null) return;
        for (int i = 0; i < appointmentList.size(); i++) {
            if (appointmentId.equals(appointmentList.get(i).getId())) {
                appointmentList.remove(i);
                saveToPrefs();
                break;
            }
        }
    }

    private long parseAppointmentTimeMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return -1L;
        String clean = timeStr.trim().replace("T", " ");
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"};
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
                sdf.setLenient(false);
                java.util.Date parsed = sdf.parse(clean.length() > pattern.length() ? clean.substring(0, pattern.length()) : clean);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return -1L;
    }

    /**
     * Kiểm tra thú cưng của User có lịch hẹn PENDING hoặc CONFIRMED nào trùng trong khoảng ±30 phút không.
     * Chỉ xét các lịch hẹn thuộc về chính User đang đăng nhập (userId).
     */
    public synchronized Appointment findPetBookingConflict(Long userId, Long petId, String appointmentTimeStr) {
        if (petId == null || appointmentTimeStr == null) return null;
        long targetMs = parseAppointmentTimeMillis(appointmentTimeStr);
        if (targetMs <= 0) return null;

        for (Appointment existing : appointmentList) {
            if (existing == null || existing.getPetId() == null || !petId.equals(existing.getPetId())) {
                continue;
            }
            if (userId != null && !userId.equals(existing.getUserId())) {
                continue;
            }
            String st = existing.getStatus() != null ? existing.getStatus().trim().toUpperCase() : "PENDING";
            if (!"PENDING".equals(st) && !"CONFIRMED".equals(st)) {
                continue;
            }
            long existingMs = parseAppointmentTimeMillis(existing.getAppointmentTime());
            if (existingMs > 0 && Math.abs(targetMs - existingMs) < 30L * 60L * 1000L) {
                return existing;
            }
        }
        return null;
    }

    /**
     * Thay toàn bộ lịch hẹn của một User bằng danh sách thật từ Database (kể cả khi danh sách rỗng),
     * đồng thời xóa các lịch giả/lịch rác không có userId.
     */
    public synchronized void setUserAppointments(Long userId, List<Appointment> serverList) {
        if (serverList == null) return;
        List<Appointment> oldList = new ArrayList<>(appointmentList);
        for (Appointment serverApt : serverList) {
            if (serverApt == null) continue;
            for (Appointment localApt : oldList) {
                if (localApt == null || serverApt.getId() == null || !serverApt.getId().equals(localApt.getId())) continue;
                if (serverApt.getPetName() == null || serverApt.getPetName().trim().isEmpty()) {
                    serverApt.setPetName(localApt.getPetName());
                }
                if (serverApt.getClinicName() == null || serverApt.getClinicName().trim().isEmpty()) {
                    serverApt.setClinicName(localApt.getClinicName());
                }
                break;
            }
        }
        appointmentList.removeIf(a -> a == null
                || a.getUserId() == null
                || (userId != null && userId.equals(a.getUserId())));
        appointmentList.addAll(0, serverList);
        saveToPrefs();
    }

    /**
     * Kiểm tra trước khi Admin bấm Duyệt (CONFIRMED):
     * Chặn nếu cùng thú cưng hoặc cùng chi nhánh đã có ca CONFIRMED trong khoảng ±30 phút.
     */
    public synchronized String findAdminApprovalConflictMessage(Appointment target) {
        if (target == null || target.getAppointmentTime() == null) return null;
        long targetMs = parseAppointmentTimeMillis(target.getAppointmentTime());
        String targetPrefix = target.getAppointmentTime().trim().length() >= 16
                ? target.getAppointmentTime().trim().substring(0, 16)
                : target.getAppointmentTime().trim();

        for (Appointment existing : appointmentList) {
            if (existing == null) continue;
            if (target.getId() != null && target.getId().equals(existing.getId())) continue;
            String st = existing.getStatus() != null ? existing.getStatus().trim().toUpperCase() : "";
            if (!"CONFIRMED".equals(st)) continue;

            String existingTime = existing.getAppointmentTime();
            if (existingTime == null) continue;
            long existingMs = parseAppointmentTimeMillis(existingTime);

            boolean timeOverlap = false;
            if (targetMs > 0 && existingMs > 0) {
                timeOverlap = Math.abs(targetMs - existingMs) < 30L * 60L * 1000L;
            } else {
                timeOverlap = existingTime.trim().startsWith(targetPrefix);
            }

            if (timeOverlap) {
                if (target.getPetId() != null && target.getPetId().equals(existing.getPetId())) {
                    String pName = target.getPetName() != null ? target.getPetName() : ("#" + target.getPetId());
                    return "Thú cưng " + pName + " đã có Ca khám #" + existing.getId()
                            + " ĐÃ DUYỆT vào lúc " + existingTime + " (trong khoảng 30 phút). Không thể duyệt trùng lịch!";
                }
                if (target.getClinicId() != null && target.getClinicId().equals(existing.getClinicId())) {
                    String cName = target.getClinicName() != null ? target.getClinicName() : ("#" + target.getClinicId());
                    return "Chi nhánh " + cName + " đã có Ca khám #" + existing.getId()
                            + " ĐÃ DUYỆT vào lúc " + existingTime + " (trùng khung giờ 30 phút). Vui lòng từ chối hoặc đổi lịch!";
                }
            }
        }
        return null;
    }

    /**
     * Đồng bộ danh sách lịch hẹn từ Server chuẩn xác 100%,
     * cập nhật đầy đủ userId, petId, clinicId và giữ petName/clinicName nếu Server chưa gửi kèm.
     */
    public synchronized void setAppointments(List<Appointment> newList) {
        if (newList == null) return;
        List<Appointment> oldList = new ArrayList<>(appointmentList);
        for (Appointment serverApt : newList) {
            if (serverApt == null) continue;
            for (Appointment localApt : oldList) {
                if (localApt == null) continue;
                boolean sameId = serverApt.getId() != null && serverApt.getId().equals(localApt.getId());
                if (sameId) {
                    if (serverApt.getPetName() == null || serverApt.getPetName().trim().isEmpty()) {
                        serverApt.setPetName(localApt.getPetName());
                    }
                    if (serverApt.getClinicName() == null || serverApt.getClinicName().trim().isEmpty()) {
                        serverApt.setClinicName(localApt.getClinicName());
                    }
                    break;
                }
            }
        }
        appointmentList.clear();
        appointmentList.addAll(newList);
        saveToPrefs();
    }
}
