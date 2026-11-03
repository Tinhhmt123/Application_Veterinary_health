package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Thực thể 5: WeightLogs (Nhật ký cân nặng)
 * Phụ trách chính: Thành viên 2 (NV 2)
 */
public class WeightLog implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("pet_id")
    private Long petId;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("weight")
    private Double weight;

    @SerializedName("note")
    private String note;

    public WeightLog() {
    }

    public WeightLog(Long id, Long petId, String recordDate, Double weight, String note) {
        this.id = id;
        this.petId = petId;
        this.recordDate = recordDate;
        this.weight = weight;
        this.note = note;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
