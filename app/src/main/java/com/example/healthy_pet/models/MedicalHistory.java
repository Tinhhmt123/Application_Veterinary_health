package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Thực thể MedicalHistories (Bệnh án & Chi phí khám chữa bệnh DECIMAL 18, 0)
 * Phụ trách chính: Người 5 / Thành viên 5 (NV 4 & NV 7)
 */
public class MedicalHistory implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("appointmentId")
    private Long appointmentId;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("petId")
    private Long petId;

    @SerializedName("petName")
    private String petName;

    @SerializedName("visitDate")
    private String visitDate;

    @SerializedName("symptoms")
    private String symptoms; // Triệu chứng bệnh

    @SerializedName("diagnosis")
    private String diagnosis; // Chẩn đoán bệnh

    @SerializedName("prescription")
    private String prescription; // Đơn thuốc kê đơn

    @SerializedName("totalCost")
    private BigDecimal totalCost; // Tương ứng DECIMAL(18,0) trong SQL Server

    @SerializedName("prescriptionUrl")
    private String prescriptionUrl; // Link ảnh đơn thuốc trên Cloudinary

    @SerializedName("paymentStatus")
    private String paymentStatus; // "UNPAID" hoặc "PAID"

    @SerializedName("paymentMethod")
    private String paymentMethod; // "VIETQR" hoặc "CASH"

    public MedicalHistory() {}

    public MedicalHistory(Long id, Long appointmentId, Long petId, String visitDate,
                          String symptoms, String diagnosis, String prescription,
                          BigDecimal totalCost, String prescriptionUrl,
                          String paymentStatus, String paymentMethod) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.petId = petId;
        this.visitDate = visitDate;
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.totalCost = totalCost;
        this.prescriptionUrl = prescriptionUrl;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
    }

    public MedicalHistory(String diagnosis, String prescription, BigDecimal totalCost) {
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.totalCost = totalCost;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BigDecimal getCost() { return totalCost != null ? totalCost : BigDecimal.ZERO; }
    public void setCost(BigDecimal cost) { this.totalCost = cost; }

    public String getPrescriptionUrl() { return prescriptionUrl; }
    public void setPrescriptionUrl(String prescriptionUrl) { this.prescriptionUrl = prescriptionUrl; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
