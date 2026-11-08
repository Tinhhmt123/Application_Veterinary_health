package org.example.backend_app.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * [Thành viên 1] Thực thể 1: Users (Quản lý Người dùng & Phân quyền)
 */
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    @JsonProperty("full_name")
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    @JsonProperty("email")
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "phone_number", length = 20)
    @JsonProperty("phone_number")
    private String phoneNumber;

    @Column(name = "avatar_url", length = 500)
    @JsonProperty("avatar_url")
    private String avatarUrl;

    @Column(name = "role", nullable = false, length = 20)
    @JsonProperty("role")
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @JsonProperty("is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public User() {
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
