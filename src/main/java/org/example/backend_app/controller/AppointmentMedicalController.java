package org.example.backend_app.controller;

import org.example.backend_app.dto.AppointmentMedicalStatistics;
import org.example.backend_app.entity.Appointment;
import org.example.backend_app.entity.MedicalHistory;
import org.example.backend_app.service.AppointmentMedicalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AppointmentMedicalController {

    private final AppointmentMedicalService appointmentMedicalService;

    public AppointmentMedicalController(AppointmentMedicalService appointmentMedicalService) {
        this.appointmentMedicalService = appointmentMedicalService;
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> getAppointmentsByUser(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "user_id", required = false) Long userIdSnake
    ) {
        return ResponseEntity.ok(appointmentMedicalService.getAppointmentsByUser(firstNonNull(userId, userIdSnake)));
    }

    @GetMapping("/appointments/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable("id") Long appointmentId) {
        return ResponseEntity.ok(appointmentMedicalService.getAppointmentById(appointmentId));
    }

    @PostMapping("/appointments")
    public ResponseEntity<Appointment> createAppointment(@RequestBody Appointment appointment) {
        return ResponseEntity.ok(appointmentMedicalService.createAppointment(appointment));
    }

    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable("id") Long appointmentId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "user_id", required = false) Long userIdSnake
    ) {
        return ResponseEntity.ok(appointmentMedicalService.cancelAppointment(
                appointmentId, firstNonNull(userId, userIdSnake)
        ));
    }

    @GetMapping("/admin/appointments")
    public ResponseEntity<List<Appointment>> getAllAppointments(
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(appointmentMedicalService.getAllAppointments(status));
    }

    @PutMapping("/admin/appointments/{id}/status")
    public ResponseEntity<Appointment> updateAppointmentStatus(
            @PathVariable("id") Long appointmentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "newStatus", required = false) String newStatus
    ) {
        return ResponseEntity.ok(appointmentMedicalService.updateAppointmentStatus(
                appointmentId, firstNonNull(status, newStatus)
        ));
    }

    @PostMapping(value = "/admin/appointments/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalHistory> completeAppointmentAndPrescribe(
            @PathVariable("id") Long appointmentId,
            @RequestParam(value = "pet_id", required = false) Long petId,
            @RequestParam(value = "petId", required = false) Long petIdCamel,
            @RequestParam(value = "symptoms", required = false) String symptoms,
            @RequestParam(value = "diagnosis", required = false) String diagnosis,
            @RequestParam(value = "prescription", required = false) String prescription,
            @RequestParam(value = "cost", required = false) BigDecimal cost,
            @RequestPart(value = "prescriptionImage", required = false) MultipartFile prescriptionImage,
            @RequestPart(value = "prescription_image", required = false) MultipartFile prescriptionImageSnake
    ) {
        MultipartFile image = prescriptionImage != null ? prescriptionImage : prescriptionImageSnake;
        return ResponseEntity.ok(appointmentMedicalService.completeAppointmentAndPrescribe(
                appointmentId, firstNonNull(petId, petIdCamel), symptoms, diagnosis, prescription, cost, image
        ));
    }

    @GetMapping("/medical-histories")
    public ResponseEntity<List<MedicalHistory>> getMedicalHistoriesByPet(
            @RequestParam(value = "petId", required = false) Long petId,
            @RequestParam(value = "pet_id", required = false) Long petIdSnake,
            @RequestParam(value = "paymentStatus", required = false) String paymentStatus
    ) {
        return ResponseEntity.ok(appointmentMedicalService.getMedicalHistories(
                firstNonNull(petId, petIdSnake), paymentStatus
        ));
    }

    @GetMapping("/medical-histories/{id}")
    public ResponseEntity<MedicalHistory> getMedicalHistoryById(@PathVariable("id") Long medicalHistoryId) {
        return ResponseEntity.ok(appointmentMedicalService.getMedicalHistoryById(medicalHistoryId));
    }

    @GetMapping("/admin/medical-histories")
    public ResponseEntity<List<MedicalHistory>> getAllMedicalHistories(
            @RequestParam(value = "petId", required = false) Long petId,
            @RequestParam(value = "pet_id", required = false) Long petIdSnake,
            @RequestParam(value = "paymentStatus", required = false) String paymentStatus
    ) {
        return ResponseEntity.ok(appointmentMedicalService.getMedicalHistories(
                firstNonNull(petId, petIdSnake), paymentStatus
        ));
    }

    @PutMapping("/medical-histories/{id}/pay")
    public ResponseEntity<MedicalHistory> payMedicalHistory(
            @PathVariable("id") Long medicalHistoryId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "payment_method", required = false) String paymentMethodSnake
    ) {
        return ResponseEntity.ok(appointmentMedicalService.payMedicalHistory(
                medicalHistoryId, firstNonNull(paymentMethod, paymentMethodSnake)
        ));
    }

    @GetMapping({"/admin/statistics", "/admin/appointment-statistics"})
    public ResponseEntity<AppointmentMedicalStatistics> getStatistics(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(appointmentMedicalService.getStatistics(from, to));
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
