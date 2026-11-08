package org.example.backend_app.service;

import org.example.backend_app.entity.Appointment;
import org.example.backend_app.entity.MedicalHistory;
import org.example.backend_app.repository.AppointmentRepository;
import org.example.backend_app.repository.MedicalHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [Thành viên 5] Service xử lý Đặt lịch hẹn (NV 5b), Duyệt lịch & Kê đơn liên thông (NV 7),
 * Bệnh án, Thanh toán Viện phí (VIETQR / CASH) & Thống kê chi phí DECIMAL(18, 0) (NV 4).
 */
@Service
public class AppointmentMedicalService {

    private final AppointmentRepository appointmentRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final CloudinaryService cloudinaryService;

    public AppointmentMedicalService(AppointmentRepository appointmentRepository,
                                     MedicalHistoryRepository medicalHistoryRepository,
                                     CloudinaryService cloudinaryService) {
        this.appointmentRepository = appointmentRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<Appointment> getAppointmentsByUser(Long userId) {
        return appointmentRepository.findByUserIdOrderByAppointmentTimeDesc(userId);
    }

    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        if (appointment.getAppointmentTime() == null) {
            appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
        }
        if (appointment.getEstimatedCost() == null) {
            appointment.setEstimatedCost(BigDecimal.ZERO);
        }
        appointment.setStatus("PENDING");
        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAllAppointments(String status) {
        if (status == null || status.isBlank()) {
            return appointmentRepository.findAllByOrderByAppointmentTimeDesc();
        }
        return appointmentRepository.findByStatusOrderByAppointmentTimeDesc(status.trim().toUpperCase());
    }

    @Transactional
    public Appointment updateAppointmentStatus(Long appointmentId, String newStatus) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn #" + appointmentId));

        appointment.setStatus(newStatus.trim().toUpperCase());
        return appointmentRepository.save(appointment);
    }

    /**
     * Nghiệp vụ 7 (Liên thông): Admin bấm Hoàn thành ca khám (COMPLETED)
     * -> Tự động tạo bản ghi MedicalHistories với trạng thái payment_status = 'UNPAID'.
     */
    @Transactional
    public MedicalHistory completeAppointmentAndPrescribe(Long appointmentId, Long petId,
                                                          String symptoms, String diagnosis,
                                                          String prescription, BigDecimal cost,
                                                          MultipartFile prescriptionImage) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn #" + appointmentId));

        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);

        MedicalHistory history = new MedicalHistory();
        history.setPetId(petId != null ? petId : appointment.getPetId());
        history.setAppointmentId(appointment.getId());
        history.setVisitDate(LocalDate.now());
        history.setSymptoms(symptoms);
        history.setDiagnosis(diagnosis);
        history.setPrescription(prescription);
        history.setCost(cost != null ? cost : BigDecimal.ZERO);
        history.setPaymentStatus("UNPAID");
        history.setPaymentMethod(null);

        if (prescriptionImage != null && !prescriptionImage.isEmpty()) {
            history.setPrescriptionImageUrl(cloudinaryService.uploadImage(prescriptionImage, "prescriptions"));
        }

        return medicalHistoryRepository.save(history);
    }

    public List<MedicalHistory> getMedicalHistoriesByPet(Long petId) {
        return medicalHistoryRepository.findByPetIdOrderByVisitDateDescIdDesc(petId);
    }

    /**
     * Nghiệp vụ 4: Thanh toán viện phí (nhận phương thức từ Strategy Pattern bên Android: "VIETQR" hoặc "CASH").
     */
    @Transactional
    public MedicalHistory payMedicalHistory(Long medicalHistoryId, String paymentMethod) {
        MedicalHistory history = medicalHistoryRepository.findById(medicalHistoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án #" + medicalHistoryId));

        String normalizedMethod = (paymentMethod != null && !paymentMethod.isBlank())
                ? paymentMethod.trim().toUpperCase()
                : "VIETQR";

        if (!"VIETQR".equals(normalizedMethod) && !"CASH".equals(normalizedMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán chỉ chấp nhận VIETQR hoặc CASH!");
        }

        history.setPaymentStatus("PAID");
        history.setPaymentMethod(normalizedMethod);
        return medicalHistoryRepository.save(history);
    }
}
