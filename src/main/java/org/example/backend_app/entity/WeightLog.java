package org.example.backend_app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * [Thành viên 2] Thực thể 5: WeightLogs (Nhật ký Cân nặng)
 */
@Entity
@Table(name = "WeightLogs")
public class WeightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "pet_id", nullable = false)
    @JsonProperty("pet_id")
    private Long petId;

    @Column(name = "record_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("record_date")
    private LocalDate recordDate;

    @Column(name = "weight", nullable = false)
    @JsonProperty("weight")
    private Double weight;

    @Column(name = "note", length = 255)
    @JsonProperty("note")
    private String note;

    public WeightLog() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
