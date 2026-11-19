package org.example.backend_app.service;

import org.example.backend_app.dto.AppointmentMedicalStatistics;
import org.example.backend_app.entity.Appointment;
import org.example.backend_app.entity.MedicalHistory;
import org.example.backend_app.entity.Pet;
import org.example.backend_app.entity.User;
import org.example.backend_app.repository.AppointmentRepository;
import org.example.backend_app.repository.MedicalHistoryRepository;
import org.example.backend_app.repository.PetRepository;
import org.example.backend_app.repository.UserRepository;
import org.example.backend_app.repository.VetClinicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AppointmentMedicalService {

    private static final Set<String> APPOINTMENT_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"
    );
    private static final Set<String> ACTIVE_APPOINTMENT_STATUSES = Set.of("PENDING", "CONFIRMED");
    private static final Set<String> PAYMENT_METHODS = Set.of("VIETQR", "CASH");
    private static final Set<String> PAYMENT_STATUSES = Set.of("UNPAID", "PAID");
    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999999999");

    private final AppointmentRepository appointmentRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final VetClinicRepository vetClinicRepository;
    private final CloudinaryService cloudinaryService;

    public AppointmentMedicalService(AppointmentRepository appointmentRepository,
                                     MedicalHistoryRepository medicalHistoryRepository,
                                     PetRepository petRepository,
                                     UserRepository userRepository,
                                     VetClinicRepository vetClinicRepository,
                                     CloudinaryService cloudinaryService) {
        this.appointmentRepository = appointmentRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.vetClinicRepository = vetClinicRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<Appointment> getAppointmentsByUser(Long userId) {
        requireUser(userId, false);
        return appointmentRepository.findByUserIdOrderByAppointmentTimeDesc(userId);
    }

    public Appointment getAppointmentById(Long appointmentId) {
        return findAppointment(appointmentId);
    }

    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        if (appointment == null) {
            throw badRequest("Dữ liệu đặt lịch không hợp lệ!");
        }
        if (appointment.getUserId() == null || appointment.getPetId() == null || appointment.getClinicId() == null) {
            throw badRequest("user_id, pet_id và clinic_id là bắt buộc!");
        }
        if (appointment.getAppointmentTime() == null) {
            throw badRequest("appointment_time là bắt buộc!");
        }
        if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
            throw badRequest("Không thể đặt lịch trong quá khứ!");
        }

        User user = requireUser(appointment.getUserId(), true);
        Pet pet = requirePet(appointment.getPetId());
        if (!appointment.getUserId().equals(pet.getUserId())) {
            throw badRequest("Thú cưng không thuộc tài khoản đặt lịch!");
        }
        if (!vetClinicRepository.existsById(appointment.getClinicId())) {
            throw notFound("Không tìm thấy phòng khám #" + appointment.getClinicId());
        }
        String serviceType = trimToNull(appointment.getServiceType());
        if (serviceType == null) {
            throw badRequest("service_type là bắt buộc!");
        }
        validateText(serviceType, "service_type", 100);
        validateText(appointment.getNote(), "note", 500);

        BigDecimal estimatedCost = normalizeMoney(appointment.getEstimatedCost(), "estimated_cost");
        if (appointmentRepository.existsByPetIdAndAppointmentTimeAndStatusIn(
                appointment.getPetId(), appointment.getAppointmentTime(), ACTIVE_APPOINTMENT_STATUSES)) {
            throw conflict("Thú cưng đã có lịch hẹn đang chờ hoặc đã xác nhận vào thời gian này!");
        }

        appointment.setId(null);
        appointment.setEstimatedCost(estimatedCost);
        appointment.setStatus("PENDING");
        appointment.setUserId(user.getId());
        appointment.setPetId(pet.getId());
        appointment.setServiceType(serviceType);
        appointment.setNote(trimToNull(appointment.getNote()));
        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAllAppointments(String status) {
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus == null) {
            return appointmentRepository.findAllByOrderByAppointmentTimeDesc();
        }
        normalizedStatus = normalizedStatus.toUpperCase(Locale.ROOT);
        validateAppointmentStatus(normalizedStatus);
        return appointmentRepository.findByStatusOrderByAppointmentTimeDesc(normalizedStatus);
    }

    @Transactional
    public Appointment updateAppointmentStatus(Long appointmentId, String newStatus) {
        Appointment appointment = findAppointment(appointmentId);
        String nextStatus = normalizeStatus(newStatus);
        String currentStatus = normalizeStatus(appointment.getStatus());

        if (!canTransition(currentStatus, nextStatus)) {
            throw conflict("Không thể chuyển lịch hẹn từ " + currentStatus + " sang " + nextStatus + "!");
        }
        if ("COMPLETED".equals(nextStatus)
                && !medicalHistoryRepository.existsByAppointmentId(appointmentId)) {
            throw badRequest("Hãy dùng API hoàn tất lịch hẹn để tạo bệnh án và kê đơn liên thông!");
        }
        appointment.setStatus(nextStatus);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, Long userId) {
        Appointment appointment = findAppointment(appointmentId);
        requireUser(userId, false);
        if (!userId.equals(appointment.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy lịch hẹn này!");
        }
        if (!"PENDING".equals(normalizeStatus(appointment.getStatus()))
                && !"CONFIRMED".equals(normalizeStatus(appointment.getStatus()))) {
            throw conflict("Chỉ có thể hủy lịch đang chờ duyệt hoặc đã xác nhận!");
        }
        appointment.setStatus("CANCELLED");
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public MedicalHistory completeAppointmentAndPrescribe(Long appointmentId, Long petId,
                                                           String symptoms, String diagnosis,
                                                           String prescription, BigDecimal cost,
                                                           MultipartFile prescriptionImage) {
        Appointment appointment = findAppointment(appointmentId);
        String currentStatus = normalizeStatus(appointment.getStatus());
        if ("CANCELLED".equals(currentStatus)) {
            throw conflict("Không thể hoàn tất lịch hẹn đã hủy!");
        }
        validateText(diagnosis, "diagnosis", 500);
        validateText(symptoms, "symptoms", 500);
        validateText(prescription, "prescription", 1000);
        String normalizedDiagnosis = trimToNull(diagnosis);
        if (normalizedDiagnosis == null) {
            throw badRequest("diagnosis là bắt buộc!");
        }

        Long resolvedPetId = petId != null ? petId : appointment.getPetId();
        if (resolvedPetId == null || !resolvedPetId.equals(appointment.getPetId())) {
            throw badRequest("pet_id phải trùng với thú cưng trong lịch hẹn!");
        }
        requirePet(resolvedPetId);

        BigDecimal finalCost = cost != null ? cost : appointment.getEstimatedCost();
        finalCost = normalizeMoney(finalCost, "cost");

        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);

        MedicalHistory history = medicalHistoryRepository.findByAppointmentId(appointmentId)
                .orElseGet(MedicalHistory::new);
        history.setPetId(resolvedPetId);
        history.setAppointmentId(appointment.getId());
        history.setVisitDate(history.getVisitDate() != null ? history.getVisitDate() : LocalDate.now());
        history.setSymptoms(trimToNull(symptoms));
        history.setDiagnosis(normalizedDiagnosis);
        history.setPrescription(trimToNull(prescription));
        history.setCost(finalCost);
        if (!"PAID".equals(normalizePaymentStatus(history.getPaymentStatus()))) {
            history.setPaymentStatus("UNPAID");
            history.setPaymentMethod(null);
        }

        if (prescriptionImage != null && !prescriptionImage.isEmpty()) {
            try {
                history.setPrescriptionImageUrl(
                        cloudinaryService.uploadImage(prescriptionImage, "prescriptions")
                );
            } catch (IllegalArgumentException ex) {
                throw badRequest(ex.getMessage());
            }
        }
        return medicalHistoryRepository.save(history);
    }

    public List<MedicalHistory> getMedicalHistoriesByPet(Long petId) {
        requirePet(petId);
        return medicalHistoryRepository.findByPetIdOrderByVisitDateDescIdDesc(petId);
    }

    public MedicalHistory getMedicalHistoryById(Long medicalHistoryId) {
        return medicalHistoryRepository.findById(medicalHistoryId)
                .orElseThrow(() -> notFound("Không tìm thấy bệnh án #" + medicalHistoryId));
    }

    public List<MedicalHistory> getMedicalHistories(Long petId, String paymentStatus) {
        String normalizedStatus = trimToNull(paymentStatus);
        if (petId != null) {
            List<MedicalHistory> histories = getMedicalHistoriesByPet(petId);
            if (normalizedStatus == null) {
                return histories;
            }
            String expectedStatus = normalizePaymentStatus(normalizedStatus);
            return histories.stream()
                    .filter(history -> expectedStatus.equals(normalizePaymentStatus(history.getPaymentStatus())))
                    .toList();
        }
        if (normalizedStatus == null) {
            return medicalHistoryRepository.findAllByOrderByVisitDateDescIdDesc();
        }
        normalizedStatus = normalizePaymentStatus(normalizedStatus);
        return medicalHistoryRepository.findByPaymentStatusOrderByVisitDateDescIdDesc(normalizedStatus);
    }

    @Transactional
    public MedicalHistory payMedicalHistory(Long medicalHistoryId, String paymentMethod) {
        MedicalHistory history = getMedicalHistoryById(medicalHistoryId);
        String normalizedMethod = trimToNull(paymentMethod);
        if (normalizedMethod == null) {
            normalizedMethod = "VIETQR";
        }
        normalizedMethod = normalizedMethod.toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(normalizedMethod)) {
            throw badRequest("Phương thức thanh toán chỉ chấp nhận VIETQR hoặc CASH!");
        }
        String currentStatus = normalizePaymentStatus(history.getPaymentStatus());
        if ("PAID".equals(currentStatus) && !normalizedMethod.equals(history.getPaymentMethod())) {
            throw conflict("Bệnh án đã được thanh toán bằng " + history.getPaymentMethod() + "!");
        }
        history.setPaymentStatus("PAID");
        history.setPaymentMethod(normalizedMethod);
        return medicalHistoryRepository.save(history);
    }

    public AppointmentMedicalStatistics getStatistics(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw badRequest("from không được lớn hơn to!");
        }

        List<Appointment> appointments;
        List<MedicalHistory> histories;
        if (from == null && to == null) {
            appointments = appointmentRepository.findAllByOrderByAppointmentTimeDesc();
            histories = medicalHistoryRepository.findAllByOrderByVisitDateDescIdDesc();
        } else {
            LocalDate startDate = from != null ? from : LocalDate.of(1970, 1, 1);
            LocalDate endDate = to != null ? to : LocalDate.now().plusYears(100);
            appointments = appointmentRepository.findByAppointmentTimeBetweenOrderByAppointmentTimeDesc(
                    startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay().minusNanos(1)
            );
            histories = medicalHistoryRepository.findByVisitDateBetweenOrderByVisitDateDescIdDesc(
                    startDate, endDate
            );
        }

        long pending = countAppointments(appointments, "PENDING");
        long confirmed = countAppointments(appointments, "CONFIRMED");
        long completed = countAppointments(appointments, "COMPLETED");
        long cancelled = countAppointments(appointments, "CANCELLED");
        BigDecimal appointmentValue = appointments.stream()
                .map(Appointment::getEstimatedCost)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidHistories = countHistories(histories, "PAID");
        long unpaidHistories = countHistories(histories, "UNPAID");
        BigDecimal totalMedicalCost = sumHistoryCost(histories);
        BigDecimal paidMedicalCost = sumHistoryCostByStatus(histories, "PAID");
        BigDecimal unpaidMedicalCost = sumHistoryCostByStatus(histories, "UNPAID");

        return new AppointmentMedicalStatistics(
                appointments.size(), pending, confirmed, completed, cancelled, appointmentValue,
                histories.size(), paidHistories, unpaidHistories,
                totalMedicalCost, paidMedicalCost, unpaidMedicalCost
        );
    }

    private Appointment findAppointment(Long appointmentId) {
        if (appointmentId == null) {
            throw badRequest("appointment_id là bắt buộc!");
        }
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> notFound("Không tìm thấy lịch hẹn #" + appointmentId));
    }

    private User requireUser(Long userId, boolean requireActive) {
        if (userId == null) {
            throw badRequest("user_id là bắt buộc!");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> notFound("Không tìm thấy người dùng #" + userId));
        if (requireActive && Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa!");
        }
        return user;
    }

    private Pet requirePet(Long petId) {
        if (petId == null) {
            throw badRequest("pet_id là bắt buộc!");
        }
        return petRepository.findById(petId)
                .orElseThrow(() -> notFound("Không tìm thấy thú cưng #" + petId));
    }

    private void validateAppointmentStatus(String status) {
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!APPOINTMENT_STATUSES.contains(normalizedStatus)) {
            throw badRequest("status chỉ chấp nhận PENDING, CONFIRMED, COMPLETED hoặc CANCELLED!");
        }
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus == null) {
            throw badRequest("status là bắt buộc!");
        }
        normalizedStatus = normalizedStatus.toUpperCase(Locale.ROOT);
        validateAppointmentStatus(normalizedStatus);
        return normalizedStatus;
    }

    private String normalizePaymentStatus(String status) {
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus == null) {
            return "UNPAID";
        }
        normalizedStatus = normalizedStatus.toUpperCase(Locale.ROOT);
        if (!PAYMENT_STATUSES.contains(normalizedStatus)) {
            throw badRequest("payment_status chỉ chấp nhận UNPAID hoặc PAID!");
        }
        return normalizedStatus;
    }

    private boolean canTransition(String currentStatus, String nextStatus) {
        if (currentStatus.equals(nextStatus)) {
            return true;
        }
        return switch (currentStatus) {
            case "PENDING" -> Set.of("CONFIRMED", "COMPLETED", "CANCELLED").contains(nextStatus);
            case "CONFIRMED" -> Set.of("COMPLETED", "CANCELLED").contains(nextStatus);
            default -> false;
        };
    }

    private void validateText(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw badRequest(fieldName + " không được vượt quá " + maxLength + " ký tự!");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
        BigDecimal normalizedValue = value != null ? value : BigDecimal.ZERO;
        if (normalizedValue.signum() < 0 || normalizedValue.compareTo(MAX_MONEY) > 0) {
            throw badRequest(fieldName + " phải nằm trong khoảng từ 0 đến 999999999999999999!");
        }
        if (normalizedValue.remainder(BigDecimal.ONE).signum() != 0) {
            throw badRequest(fieldName + " phải là số nguyên vì database dùng DECIMAL(18,0)!");
        }
        return normalizedValue.setScale(0);
    }

    private long countAppointments(Collection<Appointment> appointments, String status) {
        return appointments.stream().filter(appointment -> status.equals(normalizeStatus(appointment.getStatus()))).count();
    }

    private long countHistories(Collection<MedicalHistory> histories, String status) {
        return histories.stream().filter(history -> status.equals(normalizePaymentStatus(history.getPaymentStatus()))).count();
    }

    private BigDecimal sumHistoryCost(Collection<MedicalHistory> histories) {
        return histories.stream()
                .map(MedicalHistory::getCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumHistoryCostByStatus(Collection<MedicalHistory> histories, String status) {
        return histories.stream()
                .filter(history -> status.equals(normalizePaymentStatus(history.getPaymentStatus())))
                .map(MedicalHistory::getCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
