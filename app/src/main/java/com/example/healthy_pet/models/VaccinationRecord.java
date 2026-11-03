package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Thực thể 4: VaccinationRecords (Sổ tiêm chủng & Cảnh báo 7 ngày)
 * Phụ trách chính: Thành viên 3 (NV 3)
 */
public class VaccinationRecord implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("pet_id")
    private Long petId;

    @SerializedName("vaccine_name")
    private String vaccineName;

    @SerializedName("injection_date")
    private String injectionDate;

    @SerializedName("next_due_date")
    private String nextDueDate;

    @SerializedName("clinic_name")
    private String clinicName;

    @SerializedName("stamp_image_url")
    private String stampImageUrl;

    public VaccinationRecord() {
    }

    public VaccinationRecord(Long id, Long petId, String vaccineName, String injectionDate, String nextDueDate, String clinicName, String stampImageUrl) {
        this.id = id;
        this.petId = petId;
        this.vaccineName = vaccineName;
        this.injectionDate = injectionDate;
        this.nextDueDate = nextDueDate;
        this.clinicName = clinicName;
        this.stampImageUrl = stampImageUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }

    public String getInjectionDate() { return injectionDate; }
    public void setInjectionDate(String injectionDate) { this.injectionDate = injectionDate; }

    public String getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(String nextDueDate) { this.nextDueDate = nextDueDate; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getStampImageUrl() { return stampImageUrl; }
    public void setStampImageUrl(String stampImageUrl) { this.stampImageUrl = stampImageUrl; }
}
