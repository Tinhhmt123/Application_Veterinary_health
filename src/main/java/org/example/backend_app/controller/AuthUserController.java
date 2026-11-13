package org.example.backend_app.controller;

import jakarta.validation.Valid;
import org.example.backend_app.entity.User;
import org.example.backend_app.service.AuthUserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * [Thành viên 1] REST Controller Quản lý Tài khoản User (NV 1) & Admin Quản lý Users (NV 6a)
 * Khớp 100% với AuthUserApi.java bên App Android.
 */
@RestController
@RequestMapping("/api")
public class AuthUserController {

    private final AuthUserService authUserService;

    public AuthUserController(AuthUserService authUserService) {
        this.authUserService = authUserService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<User> login(@RequestBody User loginRequest) {
        return ResponseEntity.ok(authUserService.login(loginRequest));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<User> register(@Valid @RequestBody User registerRequest) {
        return ResponseEntity.ok(authUserService.register(registerRequest));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(authUserService.getUserById(userId));
    }

    @PutMapping(value = "/users/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> updateProfile(
            @PathVariable("id") Long userId,
            @RequestParam(value = "full_name", required = false) String fullName,
            @RequestParam(value = "phone_number", required = false) String phoneNumber,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile
    ) {
        return ResponseEntity.ok(authUserService.updateProfile(userId, fullName, phoneNumber, avatarFile));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "is_active", required = false) Boolean isActive
    ) {
        return ResponseEntity.ok(authUserService.searchUsers(keyword, isActive));
    }

    @PutMapping("/admin/users/{id}/toggle-active")
    public ResponseEntity<User> toggleUserActive(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(authUserService.toggleUserActive(userId));
    }
}
