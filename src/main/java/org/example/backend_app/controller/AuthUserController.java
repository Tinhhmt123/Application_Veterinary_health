
package org.example.backend_app.controller;

import jakarta.validation.Valid;
import org.example.backend_app.entity.User;
import org.example.backend_app.security.AccessTokenService;
import org.example.backend_app.service.AuthUserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * [Thành viên 1] REST Controller Quản lý tài khoản User
 * và Admin quản lý Users.
 */
@RestController
@RequestMapping("/api")
public class AuthUserController {

    private final AuthUserService authUserService;
    private final AccessTokenService accessTokenService;

    public AuthUserController(
            AuthUserService authUserService,
            AccessTokenService accessTokenService
    ) {
        this.authUserService = authUserService;
        this.accessTokenService = accessTokenService;
    }

    /**
     * Tạo Access Token và gắn vào User trước khi trả JSON.
     */
    private User attachAccessToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalStateException(
                    "Không thể cấp Access Token cho tài khoản không hợp lệ."
            );
        }

        String role = user.getRole() != null
                ? user.getRole().toUpperCase()
                : "USER";

        String token = accessTokenService.issue(
                user.getId(),
                role
        );

        user.setAccessToken(token);

        return user;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<User> login(
            @RequestBody User loginRequest
    ) {
        User user = authUserService.login(loginRequest);
        return ResponseEntity.ok(attachAccessToken(user));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<User> register(
            @Valid @RequestBody User registerRequest
    ) {
        return ResponseEntity.ok(
                authUserService.register(registerRequest)
        );
    }

    @PostMapping("/auth/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @RequestBody Map<String, String> payload
    ) {
        return ResponseEntity.ok(
                authUserService.sendOtp(payload)
        );
    }

    @PostMapping("/auth/verify-otp")
    public ResponseEntity<User> verifyOtp(
            @RequestBody Map<String, String> payload
    ) {
        User user = authUserService.verifyOtp(payload);
        return ResponseEntity.ok(attachAccessToken(user));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> payload
    ) {
        return ResponseEntity.ok(
                authUserService.resetPassword(payload)
        );
    }

    @PostMapping("/auth/google")
    public ResponseEntity<User> googleLogin(
            @RequestBody Map<String, String> payload
    ) {
        User user = authUserService.googleLogin(payload);
        return ResponseEntity.ok(attachAccessToken(user));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable("id") Long userId
    ) {
        return ResponseEntity.ok(
                authUserService.getUserById(userId)
        );
    }

    @PutMapping(
            value = "/users/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<User> updateProfile(
            @PathVariable("id") Long userId,
            @RequestParam(value = "full_name", required = false)
            String fullName,
            @RequestParam(value = "phone_number", required = false)
            String phoneNumber,
            @RequestPart(value = "avatarFile", required = false)
            MultipartFile avatarFile
    ) {
        return ResponseEntity.ok(
                authUserService.updateProfile(
                        userId,
                        fullName,
                        phoneNumber,
                        avatarFile
                )
        );
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(value = "keyword", required = false)
            String keyword,
            @RequestParam(value = "is_active", required = false)
            Boolean isActive
    ) {
        return ResponseEntity.ok(
                authUserService.searchUsers(keyword, isActive)
        );
    }

    @PutMapping("/admin/users/{id}/toggle-active")
    public ResponseEntity<User> toggleUserActive(
            @PathVariable("id") Long userId
    ) {
        return ResponseEntity.ok(
                authUserService.toggleUserActive(userId)
        );
    }
}
