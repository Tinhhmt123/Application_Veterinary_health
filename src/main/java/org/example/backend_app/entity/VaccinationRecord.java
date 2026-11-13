package org.example.backend_app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * [Thành viên 3] Thực thể 4: VaccinationRecords (Sổ tiêm chủng & Cảnh báo 7 ngày)
 */
@Entity
@Table(name = "VaccinationRecords")
public class VaccinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "pet_id", nullable = false)
    @JsonProperty("pet_id")
    private Long petId;

    @Column(name = "vaccine_name", nullable = false, length = 150)
    @JsonProperty("vaccine_name")
    private String vaccineName;

    @Column(name = "injection_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("injection_date")
    private LocalDate injectionDate;

    @Column(name = "next_due_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "clinic_name", length = 150)
    @JsonProperty("clinic_name")
    private String clinicName;

    @Column(name = "stamp_image_url", length = 500)
    @JsonProperty("stamp_image_url")
    private String stampImageUrl;

    public VaccinationRecord() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }

    public LocalDate getInjectionDate() { return injectionDate; }
    public void setInjectionDate(LocalDate injectionDate) { this.injectionDate = injectionDate; }

    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getStampImageUrl() { return stampImageUrl; }
    public void setStampImageUrl(String stampImageUrl) { this.stampImageUrl = stampImageUrl; }
}
