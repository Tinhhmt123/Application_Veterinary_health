package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Thực thể 7: MedicalHistories (Bệnh án khám chữa bệnh)
 * Phụ trách chính: Thành viên 5 (NV 4 & NV 7)
 */
public class MedicalHistory implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("pet_id")
    private Long petId;

    @SerializedName("appointment_id")
    private Long appointmentId; // Nullable khi khám trực tiếp tại quầy

    @SerializedName("visit_date")
    private String visitDate;

    @SerializedName("symptoms")
    private String symptoms;

    @SerializedName("diagnosis")
    private String diagnosis;

    @SerializedName("prescription")
    private String prescription;

    @SerializedName("cost")
    private BigDecimal cost; // DECIMAL(18, 0)

    @SerializedName("prescription_image_url")
    private String prescriptionImageUrl;

    public MedicalHistory() {
    }

    public MedicalHistory(Long id, Long petId, Long appointmentId, String visitDate, String symptoms, String diagnosis, String prescription, BigDecimal cost, String prescriptionImageUrl) {
        this.id = id;
        this.petId = petId;
        this.appointmentId = appointmentId;
        this.visitDate = visitDate;
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.cost = cost;
        this.prescriptionImageUrl = prescriptionImageUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public String getPrescriptionImageUrl() { return prescriptionImageUrl; }
    public void setPrescriptionImageUrl(String prescriptionImageUrl) { this.prescriptionImageUrl = prescriptionImageUrl; }
}
