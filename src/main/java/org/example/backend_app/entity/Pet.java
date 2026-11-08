package org.example.backend_app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * [Thành viên 2] Thực thể 2: Pets (Hồ sơ Thú cưng)
 */
@Entity
@Table(name = "Pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    @JsonProperty("name")
    private String name;

    @Column(name = "species", nullable = false, length = 50)
    @JsonProperty("species")
    private String species;

    @Column(name = "breed", length = 100)
    @JsonProperty("breed")
    private String breed;

    @Column(name = "gender", length = 20)
    @JsonProperty("gender")
    private String gender;

    @Column(name = "birth_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("birth_date")
    private LocalDate birthDate;

    @Column(name = "current_weight", nullable = false)
    @JsonProperty("current_weight")
    private Double currentWeight = 0.0;

    @Column(name = "avatar_url", length = 500)
    @JsonProperty("avatar_url")
    private String avatarUrl;

    public Pet() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public Double getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(Double currentWeight) { this.currentWeight = currentWeight; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
