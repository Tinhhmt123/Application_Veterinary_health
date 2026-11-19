package org.example.backend_app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
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
import java.time.Duration;
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
    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${app.public-base-url:http://192.168.1.122:8080}")
    private String publicBaseUrl;

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
                System.err.println("CLOUDINARY UPLOAD FAILED: "
                        + ex.getMessage());

                ex.printStackTrace();

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

        String boundary = "----HealthyPetBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] multipartBody = buildMultipartBody(boundary, file, safeFolder, safePublicId, timestamp, signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = SHARED_HTTP_CLIENT
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Matcher matcher = SECURE_URL_PATTERN.matcher(response.body());
            if (matcher.find()) {
                return matcher.group(1).replace("\\/", "/");
            }
        }
        throw new IOException("Cloudinary trả về mã lỗi " + response.statusCode() + ": " + response.body());
    }

    private byte[] buildMultipartBody(
            String boundary,
            MultipartFile file,
            String safeFolder,
            String safePublicId,
            long timestamp,
            String signature
    ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appendFormField(out, boundary, "api_key", apiKey);
        appendFormField(out, boundary, "timestamp", String.valueOf(timestamp));
        appendFormField(out, boundary, "folder", safeFolder);
        if (safePublicId != null && !safePublicId.isBlank()) {
            appendFormField(out, boundary, "public_id", safePublicId);
        }
        appendFormField(out, boundary, "signature", signature);

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.jpg";
        String safeFilename = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String mimeType = (file.getContentType() != null && !file.getContentType().isBlank())
                ? file.getContentType().replaceAll("[\\r\\n]", "")
                : "image/jpeg";

        out.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename + "\"\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(file.getBytes());
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private void appendFormField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        String safeValue = value != null ? value.replaceAll("[\\r\\n]", "") : "";
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + safeValue + "\r\n";
        out.write(part.getBytes(StandardCharsets.UTF_8));
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

            return publicBaseUrl.replaceAll("/+$", "")
                    + "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file ảnh: " + e.getMessage(), e);
        }
    }
}
