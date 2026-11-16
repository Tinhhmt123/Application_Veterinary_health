package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Thực thể 7: MedicalHistories (Bệnh án khám chữa bệnh & Chi phí DECIMAL 18, 0)
 * Khớp 100% với file Entity_Flow.docx:
 * id, pet_id, appointment_id, visit_date, symptoms, diagnosis, prescription, cost, prescription_image_url
 */
public class MedicalHistory implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("appointment_id")
    private Long appointmentId;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("pet_id")
    private Long petId;

    @SerializedName("pet_name")
    private String petName;

    @SerializedName("visit_date")
    private String visitDate;

    @SerializedName("symptoms")
    private String symptoms; // Triệu chứng bệnh

    @SerializedName("diagnosis")
    private String diagnosis; // Chẩn đoán của bác sĩ

    @SerializedName("prescription")
    private String prescription; // Đơn thuốc

    @SerializedName("cost")
    private BigDecimal cost; // Chi phí thực tế DECIMAL(18, 0)

    @SerializedName("prescription_image_url")
    private String prescriptionImageUrl; // Ảnh chụp toa thuốc trên Cloudinary

    @SerializedName("payment_status")
    private String paymentStatus; // "UNPAID" hoặc "PAID"

    @SerializedName("payment_method")
    private String paymentMethod; // "VIETQR" hoặc "CASH"

    public MedicalHistory() {}

    public MedicalHistory(Long id, Long appointmentId, Long petId, String visitDate,
                          String symptoms, String diagnosis, String prescription,
                          BigDecimal cost, String prescriptionImageUrl,
                          String paymentStatus, String paymentMethod) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.petId = petId;
        this.visitDate = visitDate;
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.cost = cost;
        this.prescriptionImageUrl = prescriptionImageUrl;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
    }

    public MedicalHistory(String diagnosis, String prescription, BigDecimal cost) {
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.cost = cost;
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

    public BigDecimal getCost() { return cost != null ? cost : BigDecimal.ZERO; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    // Tương thích code cũ
    public BigDecimal getTotalCost() { return getCost(); }
    public void setTotalCost(BigDecimal totalCost) { setCost(totalCost); }

    public String getPrescriptionImageUrl() { return prescriptionImageUrl; }
    public void setPrescriptionImageUrl(String prescriptionImageUrl) { this.prescriptionImageUrl = prescriptionImageUrl; }

    // Tương thích code cũ
    public String getPrescriptionUrl() { return prescriptionImageUrl; }
    public void setPrescriptionUrl(String url) { this.prescriptionImageUrl = url; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
