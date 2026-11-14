package org.example.backend_app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dịch vụ dùng chung cho cả 5 thành viên: Upload ảnh lên Cloudinary.
 * Hỗ trợ đặt tên file tùy chỉnh (public_id) như <Name_User>_<Id_User>.
 */
@Service
public class CloudinaryService {

    private static final Pattern SECURE_URL_PATTERN = Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    public String uploadImage(MultipartFile file, String folder) {
        return uploadImage(file, folder, null);
    }

    public String uploadImage(MultipartFile file, String folder, String customPublicId) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        validateImageFile(file);
        String safePublicId = sanitizePublicId(customPublicId);

        // Nếu đã điền đủ cấu hình Cloudinary -> Upload trực tiếp lên đám mây Cloudinary
        if (cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank()) {
            try {
                return uploadToCloudinaryCloud(file, folder, safePublicId);
            } catch (Exception ex) {
                // Nếu mất kết nối mạng tới Cloudinary, tự động lưu dự phòng vào /uploads/
                return saveLocally(file, folder, safePublicId);
            }
        }

        // Chế độ dự phòng Offline/Local khi chưa nhập Cloudinary Key
        return saveLocally(file, folder, safePublicId);
    }

    private void validateImageFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Kích thước file ảnh vượt quá giới hạn 10MB!");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !contentType.toLowerCase().startsWith("image/")
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ (chỉ chấp nhận file ảnh)!");
        }
    }

    /**
     * Chuẩn hóa tên file thành không dấu, thay khoảng trắng bằng dấu gạch dưới (VD: "Nguyễn Văn Tính_2" -> "Nguyen_Van_Tinh_2")
     */
    private String sanitizePublicId(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(rawName.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String cleaned = normalized.replaceAll("[^a-zA-Z0-9_\\-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String uploadToCloudinaryCloud(MultipartFile file, String folder, String safePublicId) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000L;
        String safeFolder = "healthy_pet/" + folder;

        // Cloudinary bắt buộc sắp xếp tham số theo thứ tự bảng chữ cái: folder -> public_id -> timestamp
        String toSign;
        if (safePublicId != null && !safePublicId.isBlank()) {
            toSign = "folder=" + safeFolder + "&public_id=" + safePublicId + "&timestamp=" + timestamp + apiSecret;
        } else {
            toSign = "folder=" + safeFolder + "&timestamp=" + timestamp + apiSecret;
        }

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        String signature = HexFormat.of().formatHex(sha1.digest(toSign.getBytes(StandardCharsets.UTF_8)));

        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());

        StringBuilder formBody = new StringBuilder();
        formBody.append("file=").append(URLEncoder.encode(dataUri, StandardCharsets.UTF_8))
                .append("&api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
                .append("&timestamp=").append(timestamp)
                .append("&folder=").append(URLEncoder.encode(safeFolder, StandardCharsets.UTF_8));
        if (safePublicId != null && !safePublicId.isBlank()) {
            formBody.append("&public_id=").append(URLEncoder.encode(safePublicId, StandardCharsets.UTF_8));
        }
        formBody.append("&signature=").append(signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Matcher matcher = SECURE_URL_PATTERN.matcher(response.body());
            if (matcher.find()) {
                return matcher.group(1).replace("\\/", "/");
            }
        }
        throw new IOException("Cloudinary trả về mã lỗi " + response.statusCode() + ": " + response.body());
    }

    private String saveLocally(MultipartFile file, String folder, String safePublicId) {
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
            String baseName = (safePublicId != null && !safePublicId.isBlank()) ? safePublicId : UUID.randomUUID().toString();
            String filename = baseName + ext;

            Path targetDir = Paths.get("uploads", folder).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            return "http://10.0.2.2:8080/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file ảnh: " + e.getMessage(), e);
        }
    }
}
