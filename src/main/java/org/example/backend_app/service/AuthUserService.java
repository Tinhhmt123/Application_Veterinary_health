package org.example.backend_app.service;

import jakarta.annotation.PostConstruct;
import org.example.backend_app.entity.User;
import org.example.backend_app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [Thành viên 1] Service xử lý nghiệp vụ Tài khoản User (NV 1) & Quản lý Users cho Admin (NV 6a)
 * Hỗ trợ: Kiểm tra khóa tài khoản (is_active) vs kích hoạt OTP 6 số (is_verified),
 * Quên mật khẩu qua OTP 6 số và Đăng nhập Google.
 */
@Service
public class AuthUserService {

    private static final Logger log = LoggerFactory.getLogger(AuthUserService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${google.client-id:}")
    private String googleWebClientId;

    @Value("${google.android-client-id:}")
    private String googleAndroidClientId;

    public AuthUserService(
            UserRepository userRepository,
            CloudinaryService cloudinaryService,
            JdbcTemplate jdbcTemplate,
            ObjectProvider<JavaMailSender> mailSenderProvider
    ) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.jdbcTemplate = jdbcTemplate;
        this.mailSenderProvider = mailSenderProvider;
    }

    @PostConstruct
    public void ensureUsersAuthSchema() {
        try {
            jdbcTemplate.execute(
                    "IF COL_LENGTH('dbo.Users', 'is_verified') IS NULL " +
                    "ALTER TABLE dbo.Users ADD is_verified BIT NOT NULL CONSTRAINT DF_Users_IsVerified DEFAULT 1 WITH VALUES;"
            );
            jdbcTemplate.execute(
                    "IF COL_LENGTH('dbo.Users', 'otp_code') IS NULL " +
                    "ALTER TABLE dbo.Users ADD otp_code VARCHAR(10) NULL;"
            );
            jdbcTemplate.execute(
                    "IF COL_LENGTH('dbo.Users', 'otp_expiry') IS NULL " +
                    "ALTER TABLE dbo.Users ADD otp_expiry DATETIME2 NULL;"
            );
            jdbcTemplate.execute(
                    "IF COL_LENGTH('dbo.Users', 'auth_provider') IS NULL " +
                    "ALTER TABLE dbo.Users ADD auth_provider VARCHAR(20) NOT NULL CONSTRAINT DF_Users_AuthProvider DEFAULT 'LOCAL' WITH VALUES;"
            );
            jdbcTemplate.execute(
                    "IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Users') AND name = 'password' AND is_nullable = 0) " +
                    "ALTER TABLE dbo.Users ALTER COLUMN password VARCHAR(255) NULL;"
            );
            migrateLegacyPlainTextPasswords();
        } catch (Exception ex) {
            log.warn("Không thể tự động kiểm tra/bổ sung cột OTP cho bảng dbo.Users: {}", ex.getMessage());
        }
    }

    public User login(User loginRequest) {
        if (loginRequest == null
                || loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()
                || loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ Email và Mật khẩu!");
        }
        User user = userRepository.findByEmailIgnoreCase(loginRequest.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email không tồn tại trong hệ thống!"));

        if (user.getPassword() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản này đăng nhập bằng Google! Vui lòng dùng nút Đăng nhập bằng Google hoặc Quên mật khẩu để đặt mật khẩu."
            );
        }
        if (!verifyAndUpgradePassword(user, loginRequest.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mật khẩu không chính xác!");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!");
        }
        if (Boolean.FALSE.equals(user.getIsVerified())) {
            issueAndSendOtp(user, "Kích hoạt tài khoản Healthy Pet");
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Tài khoản chưa kích hoạt! Mã OTP 6 số đã được gửi tới email " + user.getEmail()
            );
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
        if (registerRequest.getPassword() == null || registerRequest.getPassword().isBlank() || registerRequest.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có từ 6 ký tự trở lên!");
        }
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký!");
        }

        User newUser = new User();
        newUser.setFullName(registerRequest.getFullName().trim());
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(PASSWORD_ENCODER.encode(registerRequest.getPassword()));
        newUser.setPhoneNumber(registerRequest.getPhoneNumber() != null ? registerRequest.getPhoneNumber().trim() : null);
        newUser.setRole("USER");
        newUser.setIsActive(true);
        newUser.setIsVerified(false);
        newUser.setAuthProvider("LOCAL");

        User savedUser = userRepository.save(newUser);
        issueAndSendOtp(savedUser, "Kích hoạt tài khoản Healthy Pet");
        return savedUser;
    }

    @Transactional
    public Map<String, Object> sendOtp(Map<String, String> payload) {
        String email = payload != null && payload.get("email") != null ? payload.get("email").trim().toLowerCase() : "";
        String purpose = payload != null && payload.get("purpose") != null ? payload.get("purpose").trim() : "ACTIVATE";
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập địa chỉ Email!");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống!"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!");
        }

        String subject = "RESET_PASSWORD".equalsIgnoreCase(purpose)
                ? "Mã OTP đặt lại mật khẩu - Healthy Pet"
                : "Mã OTP kích hoạt tài khoản - Healthy Pet";
        issueAndSendOtp(user, subject);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", user.getEmail());
        result.put("message", "Mã OTP 6 số đã được gửi tới email " + user.getEmail() + " (hiệu lực " + OTP_EXPIRY_MINUTES + " phút).");
        return result;
    }

    @Transactional
    public User verifyOtp(Map<String, String> payload) {
        String email = payload != null && payload.get("email") != null ? payload.get("email").trim().toLowerCase() : "";
        String otpCode = payload != null && payload.get("otp_code") != null ? payload.get("otp_code").trim() : "";

        if (email.isBlank() || otpCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ Email và mã OTP 6 số!");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống!"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!");
        }

        validateUserOtp(user, otpCode);

        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        return userRepository.save(user);
    }

    @Transactional
    public Map<String, Object> resetPassword(Map<String, String> payload) {
        String email = payload != null && payload.get("email") != null ? payload.get("email").trim().toLowerCase() : "";
        String otpCode = payload != null && payload.get("otp_code") != null ? payload.get("otp_code").trim() : "";
        String newPassword = payload != null && payload.get("new_password") != null ? payload.get("new_password") : "";

        if (email.isBlank() || otpCode.isBlank() || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ Email, mã OTP và mật khẩu mới!");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có từ 6 ký tự trở lên!");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống!"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!");
        }

        validateUserOtp(user, otpCode);

        user.setPassword(PASSWORD_ENCODER.encode(newPassword));
        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", user.getEmail());
        result.put("message", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
        return result;
    }

    @Transactional
    public User googleLogin(Map<String, String> payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu đăng nhập Google không hợp lệ!");
        }
        String idToken = payload.get("id_token") != null ? payload.get("id_token").trim() : "";
        String email = payload.get("email") != null ? payload.get("email").trim().toLowerCase() : "";
        String fullName = payload.get("full_name") != null ? payload.get("full_name").trim() : "";
        String avatarUrl = payload.get("avatar_url") != null ? payload.get("avatar_url").trim() : null;

        if (!idToken.isBlank()) {
            Map<String, String> verifiedInfo = verifyGoogleIdToken(idToken);
            if (verifiedInfo.get("email") != null && !verifiedInfo.get("email").isBlank()) {
                email = verifiedInfo.get("email").trim().toLowerCase();
            }
            if ((fullName == null || fullName.isBlank()) && verifiedInfo.get("name") != null) {
                fullName = verifiedInfo.get("name").trim();
            }
            if ((avatarUrl == null || avatarUrl.isBlank()) && verifiedInfo.get("picture") != null) {
                avatarUrl = verifiedInfo.get("picture").trim();
            }
        }

        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không lấy được địa chỉ Email từ tài khoản Google!");
        }

        final String resolvedEmail = email;
        final String resolvedFullName = (fullName != null && !fullName.isBlank())
                ? fullName
                : (resolvedEmail.contains("@") ? resolvedEmail.substring(0, resolvedEmail.indexOf('@')) : "Người dùng Google");
        final String resolvedAvatarUrl = (avatarUrl != null && !avatarUrl.isBlank()) ? avatarUrl : null;

        User existingUser = userRepository.findByEmailIgnoreCase(resolvedEmail).orElse(null);
        if (existingUser != null) {
            if (Boolean.FALSE.equals(existingUser.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!");
            }
            if (Boolean.FALSE.equals(existingUser.getIsVerified())) {
                existingUser.setIsVerified(true);
                existingUser.setOtpCode(null);
                existingUser.setOtpExpiry(null);
            }
            if ((existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank()) && resolvedAvatarUrl != null) {
                existingUser.setAvatarUrl(resolvedAvatarUrl);
            }
            return userRepository.save(existingUser);
        }

        User newUser = new User();
        newUser.setFullName(resolvedFullName);
        newUser.setEmail(resolvedEmail);
        newUser.setPassword(null);
        newUser.setAvatarUrl(resolvedAvatarUrl);
        newUser.setRole("USER");
        newUser.setIsActive(true);
        newUser.setIsVerified(true);
        newUser.setAuthProvider("GOOGLE");
        return userRepository.save(newUser);
    }

    private void validateUserOtp(User user, String otpCode) {
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otpCode.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP 6 số không chính xác!");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn! Vui lòng bấm Gửi lại mã.");
        }
    }

    private void migrateLegacyPlainTextPasswords() {
        List<User> users = userRepository.findAll();
        int upgradedCount = 0;
        for (User user : users) {
            String storedPassword = user.getPassword();
            if (storedPassword != null && !storedPassword.isBlank() && !isBcryptHash(storedPassword)) {
                user.setPassword(PASSWORD_ENCODER.encode(storedPassword));
                userRepository.save(user);
                upgradedCount++;
            }
        }
        if (upgradedCount > 0) {
            log.info("Đã tự động mã hóa BCrypt cho {} tài khoản cũ trong dbo.Users.", upgradedCount);
        }
    }

    private boolean verifyAndUpgradePassword(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || rawPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        }
        if (!storedPassword.equals(rawPassword)) {
            return false;
        }
        user.setPassword(PASSWORD_ENCODER.encode(rawPassword));
        userRepository.save(user);
        return true;
    }

    private boolean isBcryptHash(String storedPassword) {
        return storedPassword != null
                && (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$"));
    }

    private String issueAndSendOtp(User user, String subject) {
        String otp = String.format("%06d", 100000 + SECURE_RANDOM.nextInt(900000));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepository.save(user);

        log.info("==================================================================");
        log.info("[HEALTHY PET OTP] Email: {} | Mã OTP 6 số: {} (Hết hạn sau {} phút)", user.getEmail(), otp, OTP_EXPIRY_MINUTES);
        log.info("==================================================================");

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank()) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(mailUsername.trim());
                mail.setTo(user.getEmail());
                mail.setSubject(subject);
                mail.setText(
                        "Xin chào " + user.getFullName() + ",\n\n" +
                        "Mã xác thực OTP 6 số của bạn trên ứng dụng Healthy Pet là: " + otp + "\n" +
                        "Mã có hiệu lực trong vòng " + OTP_EXPIRY_MINUTES + " phút.\n\n" +
                        "Trân trọng,\nĐội ngũ Healthy Pet"
                );
                mailSender.send(mail);
                log.info("Đã gửi email OTP thành công tới {}", user.getEmail());
            } catch (Exception ex) {
                log.warn("Không thể gửi email qua SMTP (vui lòng kiểm tra MAIL_USERNAME/MAIL_PASSWORD trong .env). Đã in mã OTP {} ra Console: {}", otp, ex.getMessage());
            }
        } else {
            log.info("Chưa cấu hình MAIL_USERNAME/MAIL_PASSWORD trong .env -> Sử dụng mã OTP in trên Console: {}", otp);
        }
        return otp;
    }

    private Map<String, String> verifyGoogleIdToken(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google không hợp lệ hoặc đã hết hạn!");
            }
            String json = response.body();
            String aud = extractJsonString(json, "aud");
            String azp = extractJsonString(json, "azp");
            boolean hasWebClient = googleWebClientId != null && !googleWebClientId.isBlank();
            boolean hasAndroidClient = googleAndroidClientId != null && !googleAndroidClientId.isBlank();
            if (hasWebClient || hasAndroidClient) {
                boolean matched = (hasWebClient && (googleWebClientId.equals(aud) || googleWebClientId.equals(azp)))
                        || (hasAndroidClient && (googleAndroidClientId.equals(aud) || googleAndroidClientId.equals(azp)));
                if (!matched) {
                    log.warn("Google token aud/azp ({} / {}) khác cấu hình .env, vẫn cho phép theo email đã xác thực Google.", aud, azp);
                }
            }
            Map<String, String> info = new LinkedHashMap<>();
            info.put("email", extractJsonString(json, "email"));
            info.put("name", extractJsonString(json, "name"));
            info.put("picture", extractJsonString(json, "picture"));
            return info;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Đăng nhập thất bại! Vui long thử lại sau");
            return Map.of();
        }
    }

    private String extractJsonString(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return "";
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
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
                String customImageName = user.getFullName() + "_" + user.getId();
                String uploadedUrl = cloudinaryService.uploadImage(avatarFile, "avatars", customImageName);
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
