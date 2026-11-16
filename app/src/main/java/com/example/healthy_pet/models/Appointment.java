package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Thực thể 6: Appointments (Lịch hẹn khám & Spa)
 * Khớp 100% với file Entity_Flow.docx:
 * id, user_id, pet_id, clinic_id, service_type, appointment_time, estimated_cost, status, note
 */
public class Appointment implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("pet_id")
    private Long petId;

    @SerializedName("pet_name")
    private String petName;

    @SerializedName("clinic_id")
    private Long clinicId;

    @SerializedName("clinic_name")
    private String clinicName;

    @SerializedName("service_type")
    private String serviceType; // Khám bệnh, Tiêm phòng, Spa...

    @SerializedName("appointment_time")
    private String appointmentTime; // Định dạng "yyyy-MM-dd HH:mm:ss"

    @SerializedName("estimated_cost")
    private BigDecimal estimatedCost; // Chi phí dự kiến DECIMAL(18, 0)

    @SerializedName("status")
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @SerializedName("note")
    private String note; // Ghi chú triệu chứng ban đầu

    public Appointment() {}

    public Appointment(Long userId, Long petId, Long clinicId, String serviceType, String appointmentTime, BigDecimal estimatedCost, String note) {
        this.userId = userId;
        this.petId = petId;
        this.clinicId = clinicId;
        this.serviceType = serviceType;
        this.appointmentTime = appointmentTime;
        this.estimatedCost = estimatedCost;
        this.note = note;
        this.status = "PENDING";
    }

    // Constructor hỗ trợ tương thích cũ
    public Appointment(Long userId, Long petId, Long clinicId, String appointmentDate, String appointmentTime, String reason) {
        this.userId = userId;
        this.petId = petId;
        this.clinicId = clinicId;
        this.serviceType = "Khám bệnh";
        this.appointmentTime = (appointmentDate != null && appointmentTime != null) ? (appointmentDate + " " + appointmentTime) : appointmentTime;
        this.estimatedCost = new BigDecimal("200000");
        this.note = reason;
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getServiceType() { return serviceType != null ? serviceType : "Khám bệnh"; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }

    public BigDecimal getEstimatedCost() { return estimatedCost != null ? estimatedCost : BigDecimal.ZERO; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    // Phương thức trợ giúp tương thích code cũ
    public String getReason() { return note != null ? note : ""; }
    public void setReason(String reason) { this.note = reason; }

    public String getAppointmentDate() {
        if (appointmentTime != null && appointmentTime.contains(" ")) {
            return appointmentTime.split(" ")[0];
        }
        return appointmentTime != null ? appointmentTime : "";
    }
}
