package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Thực thể 6: Appointments (Lịch hẹn khám & Spa)
 * Phụ trách chính: Thành viên 5 (NV 5b & NV 7)
 */
public class Appointment implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("pet_id")
    private Long petId;

    @SerializedName("clinic_id")
    private Long clinicId;

    @SerializedName("service_type")
    private String serviceType;

    @SerializedName("appointment_time")
    private String appointmentTime;

    @SerializedName("estimated_cost")
    private BigDecimal estimatedCost; // DECIMAL(18, 0)

    @SerializedName("status")
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @SerializedName("note")
    private String note;

    public Appointment() {
    }

    public Appointment(Long id, Long userId, Long petId, Long clinicId, String serviceType, String appointmentTime, BigDecimal estimatedCost, String status, String note) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.clinicId = clinicId;
        this.serviceType = serviceType;
        this.appointmentTime = appointmentTime;
        this.estimatedCost = estimatedCost;
        this.status = status;
        this.note = note;
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

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
