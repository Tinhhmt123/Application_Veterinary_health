package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;

public class Appointment {
    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("petId")
    private Long petId;

    @SerializedName("petName")
    private String petName;

    @SerializedName("clinicId")
    private Long clinicId;

    @SerializedName("clinicName")
    private String clinicName;

    @SerializedName("appointmentDate")
    private String appointmentDate; // Định dạng yyyy-MM-dd

    @SerializedName("appointmentTime")
    private String appointmentTime; // Định dạng HH:mm:ss

    @SerializedName("reason")
    private String reason;

    @SerializedName("status")
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    public Appointment() {}

    public Appointment(Long userId, Long petId, Long clinicId, String appointmentDate, String appointmentTime, String reason) {
        this.userId = userId;
        this.petId = petId;
        this.clinicId = clinicId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
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
    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }
    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
