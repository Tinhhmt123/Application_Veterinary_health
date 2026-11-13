package org.example.backend_app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * [Thành viên 5] Thực thể 6: Appointments (Lịch hẹn Khám / Tiêm / Spa)
 */
@Entity
@Table(name = "Appointments")
public class Appointment {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId;

    @Column(name = "pet_id", nullable = false)
    @JsonProperty("pet_id")
    private Long petId;

    @Column(name = "clinic_id", nullable = false)
    @JsonProperty("clinic_id")
    private Long clinicId;

    @Column(name = "service_type", nullable = false, length = 100)
    @JsonProperty("service_type")
    private String serviceType;

    @Column(name = "appointment_time", nullable = false)
    @JsonIgnore
    private LocalDateTime appointmentTime;

    @Column(name = "estimated_cost", nullable = false, precision = 18, scale = 0)
    @JsonProperty("estimated_cost")
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 30)
    @JsonProperty("status")
    private String status = "PENDING";

    @Column(name = "note", length = 500)
    @JsonProperty("note")
    private String note;

    public Appointment() {
    }

    /**
     * Đồng bộ định dạng chuỗi thời gian "yyyy-MM-dd HH:mm" với App Android.
     */
    @JsonProperty("appointment_time")
    public String getAppointmentTimeText() {
        return appointmentTime != null ? appointmentTime.format(DISPLAY_FMT) : null;
    }

    @JsonProperty("appointment_time")
    public void setAppointmentTimeText(String text) {
        if (text == null || text.isBlank()) {
            this.appointmentTime = LocalDateTime.now();
            return;
        }
        String clean = text.trim().replace("T", " ");
        if (clean.length() == 16) {
            this.appointmentTime = LocalDateTime.parse(clean, DISPLAY_FMT);
        } else if (clean.length() >= 19) {
            this.appointmentTime = LocalDateTime.parse(clean.substring(0, 19),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            this.appointmentTime = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
