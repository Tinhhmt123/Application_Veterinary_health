package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Thực thể 1: Users (Quản lý Người dùng)
 * Phụ trách chính: Thành viên 1 (NV 1 & NV 6a)
 */
public class User implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("role")
    private String role; // "USER" hoặc "ADMIN"

    @SerializedName("is_active")
    private Boolean isActive;

    public User() {
    }

    public User(Long id, String fullName, String email, String phoneNumber, String avatarUrl, String role, Boolean isActive) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
