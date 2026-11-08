package org.example.backend_app.service;

import org.example.backend_app.entity.User;
import org.example.backend_app.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * [Thành viên 1] Service xử lý nghiệp vụ Tài khoản User (NV 1) & Quản lý Users cho Admin (NV 6a)
 */
@Service
public class AuthUserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public AuthUserService(UserRepository userRepository, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public User login(User loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ Email và Mật khẩu!");
        }
        User user = userRepository.findByEmail(loginRequest.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác!"));

        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác!");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!");
        }
        return user;
    }

    @Transactional
    public User register(User registerRequest) {
        if (registerRequest.getEmail() == null || registerRequest.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống!");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký!");
        }

        User newUser = new User();
        newUser.setFullName(registerRequest.getFullName());
        newUser.setEmail(registerRequest.getEmail().trim());
        newUser.setPassword(registerRequest.getPassword());
        newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        newUser.setRole("USER");
        newUser.setIsActive(true);
        return userRepository.save(newUser);
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String phoneNumber, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + userId));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            user.setPhoneNumber(phoneNumber.trim());
        }
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String uploadedUrl = cloudinaryService.uploadImage(avatarFile, "avatars");
            user.setAvatarUrl(uploadedUrl);
        }
        return userRepository.save(user);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchByKeyword(keyword);
    }

    @Transactional
    public User toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + userId));

        boolean current = Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(!current);
        return userRepository.save(user);
    }
}
