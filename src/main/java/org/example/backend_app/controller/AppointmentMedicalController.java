package org.example.backend_app.controller;

import org.example.backend_app.entity.Appointment;
import org.example.backend_app.entity.MedicalHistory;
import org.example.backend_app.service.AppointmentMedicalService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * [Thành viên 5] REST Controller Đặt lịch hẹn (NV 5b), Duyệt lịch & Kê đơn liên thông (NV 7),
 * Bệnh án, Thanh toán Viện phí (VIETQR / CASH) & Thống kê DECIMAL (NV 4).
 * Khớp 100% với AppointmentMedicalApi.java bên App Android.
 */
@RestController
@RequestMapping("/api")
public class AppointmentMedicalController {

    private final AppointmentMedicalService appointmentMedicalService;

    public AppointmentMedicalController(AppointmentMedicalService appointmentMedicalService) {
        this.appointmentMedicalService = appointmentMedicalService;
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> getAppointmentsByUser(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(appointmentMedicalService.getAppointmentsByUser(userId));
    }

    @PostMapping("/appointments")
    public ResponseEntity<Appointment> createAppointment(@RequestBody Appointment appointment) {
        return ResponseEntity.ok(appointmentMedicalService.createAppointment(appointment));
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
            @RequestParam("status") String newStatus
    ) {
        return ResponseEntity.ok(appointmentMedicalService.updateAppointmentStatus(appointmentId, newStatus));
    }

    @PostMapping(value = "/admin/appointments/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalHistory> completeAppointmentAndPrescribe(
            @PathVariable("id") Long appointmentId,
            @RequestParam(value = "pet_id", required = false) Long petId,
            @RequestParam(value = "symptoms", required = false) String symptoms,
            @RequestParam("diagnosis") String diagnosis,
            @RequestParam(value = "prescription", required = false) String prescription,
            @RequestParam("cost") BigDecimal cost,
            @RequestPart(value = "prescriptionImage", required = false) MultipartFile prescriptionImage
    ) {
        return ResponseEntity.ok(appointmentMedicalService.completeAppointmentAndPrescribe(
                appointmentId, petId, symptoms, diagnosis, prescription, cost, prescriptionImage
        ));
    }

    @GetMapping("/medical-histories")
    public ResponseEntity<List<MedicalHistory>> getMedicalHistoriesByPet(@RequestParam("petId") Long petId) {
        return ResponseEntity.ok(appointmentMedicalService.getMedicalHistoriesByPet(petId));
    }

    @PutMapping("/medical-histories/{id}/pay")
    public ResponseEntity<MedicalHistory> payMedicalHistory(
            @PathVariable("id") Long medicalHistoryId,
            @RequestParam("paymentMethod") String paymentMethod
    ) {
        return ResponseEntity.ok(appointmentMedicalService.payMedicalHistory(medicalHistoryId, paymentMethod));
    }
}
