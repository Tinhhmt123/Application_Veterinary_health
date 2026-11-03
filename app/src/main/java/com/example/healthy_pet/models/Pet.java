package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Thực thể 2: Pets (Quản lý Thú cưng)
 * Phụ trách chính: Thành viên 2 (NV 2 & NV 6b)
 */
public class Pet implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("name")
    private String name;

    @SerializedName("species")
    private String species; // Chó, Mèo...

    @SerializedName("breed")
    private String breed; // Poodle, Corgi, Mèo Anh...

    @SerializedName("gender")
    private String gender; // Đực / Cái

    @SerializedName("birth_date")
    private String birthDate;

    @SerializedName("current_weight")
    private Double currentWeight;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public Pet() {
    }

    public Pet(Long id, Long userId, String name, String species, String breed, String gender, String birthDate, Double currentWeight, String avatarUrl) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.gender = gender;
        this.birthDate = birthDate;
        this.currentWeight = currentWeight;
        this.avatarUrl = avatarUrl;
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

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public Double getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(Double currentWeight) { this.currentWeight = currentWeight; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
