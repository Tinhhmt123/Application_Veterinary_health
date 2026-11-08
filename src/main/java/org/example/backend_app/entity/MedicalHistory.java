package org.example.backend_app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [Thành viên 5] Thực thể 7: MedicalHistories (Bệnh án, Thanh toán VietQR/CASH & Thống kê DECIMAL)
 */
@Entity
@Table(name = "MedicalHistories")
public class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "pet_id", nullable = false)
    @JsonProperty("pet_id")
    private Long petId;

    @Column(name = "appointment_id")
    @JsonProperty("appointment_id")
    private Long appointmentId;

    @Column(name = "visit_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("visit_date")
    private LocalDate visitDate;

    @Column(name = "symptoms", length = 500)
    @JsonProperty("symptoms")
    private String symptoms;

    @Column(name = "diagnosis", nullable = false, length = 500)
    @JsonProperty("diagnosis")
    private String diagnosis;

    @Column(name = "prescription", length = 1000)
    @JsonProperty("prescription")
    private String prescription;

    @Column(name = "cost", nullable = false, precision = 18, scale = 0)
    @JsonProperty("cost")
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "prescription_image_url", length = 500)
    @JsonProperty("prescription_image_url")
    private String prescriptionImageUrl;

    @Column(name = "payment_status", nullable = false, length = 20)
    @JsonProperty("payment_status")
    private String paymentStatus = "UNPAID";

    @Column(name = "payment_method", length = 30)
    @JsonProperty("payment_method")
    private String paymentMethod;

    public MedicalHistory() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

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

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
