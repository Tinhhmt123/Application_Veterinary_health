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
        if (loginRequest == null
                || loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()
                || loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ Email và Mật khẩu!");
        }
        User user = userRepository.findByEmailIgnoreCase(loginRequest.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email không tồn tại trong hệ thống!"));

        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mật khẩu không chính xác!");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!");
        }
        return user;
    }

    @Transactional
    public User register(User registerRequest) {
        if (registerRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu đăng ký không hợp lệ!");
        }
        if (registerRequest.getFullName() == null || registerRequest.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ và tên không được để trống!");
        }
        if (registerRequest.getEmail() == null || registerRequest.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống!");
        }
        if (registerRequest.getPassword() == null || registerRequest.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có từ 6 ký tự trở lên!");
        }
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký!");
        }

        User newUser = new User();
        newUser.setFullName(registerRequest.getFullName().trim());
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(registerRequest.getPassword());
        newUser.setPhoneNumber(registerRequest.getPhoneNumber() != null ? registerRequest.getPhoneNumber().trim() : null);
        newUser.setRole("USER");
        newUser.setIsActive(true);
        return userRepository.save(newUser);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + userId));
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String phoneNumber, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + userId));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            String cleanedPhone = phoneNumber.trim();
            if (cleanedPhone.length() > 20) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại tối đa 20 ký tự!");
            }
            user.setPhoneNumber(cleanedPhone);
        }
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String uploadedUrl = cloudinaryService.uploadImage(avatarFile, "avatars");
                user.setAvatarUrl(uploadedUrl);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
        }
        return userRepository.save(user);
    }

    public List<User> searchUsers(String keyword) {
        return searchUsers(keyword, null);
    }

    public List<User> searchUsers(String keyword, Boolean isActive) {
        List<User> results = userRepository.searchByKeyword(keyword != null ? keyword.trim() : null);
        if (isActive == null) {
            return results;
        }
        return results.stream()
                .filter(u -> isActive.equals(u.getIsActive()))
                .toList();
    }

    @Transactional
    public User toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + userId));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể khóa tài khoản Quản trị viên (ADMIN)!");
        }

        boolean current = Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(!current);
        return userRepository.save(user);
    }
}
