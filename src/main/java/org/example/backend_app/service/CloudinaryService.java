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
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dịch vụ dùng chung cho cả 5 thành viên: Upload ảnh lên Cloudinary.
 * Nếu chưa cấu hình API Key Cloudinary trong application.yaml, tự động lưu dự phòng vào thư mục /uploads/
 * để nhóm vẫn test upload ảnh trên Postman và Android Emulator bình thường.
 */
@Service
public class CloudinaryService {

    private static final Pattern SECURE_URL_PATTERN = Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Nếu đã điền đủ cấu hình Cloudinary -> Upload trực tiếp lên đám mây Cloudinary
        if (cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank()) {
            try {
                return uploadToCloudinaryCloud(file, folder);
            } catch (Exception ex) {
                throw new RuntimeException("Lỗi upload ảnh lên Cloudinary: " + ex.getMessage(), ex);
            }
        }

        // Chế độ dự phòng Offline/Local khi chưa nhập Cloudinary Key
        return saveLocally(file, folder);
    }

    private String uploadToCloudinaryCloud(MultipartFile file, String folder) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000L;
        String safeFolder = "healthy_pet/" + folder;

        String toSign = "folder=" + safeFolder + "&timestamp=" + timestamp + apiSecret;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        String signature = HexFormat.of().formatHex(sha1.digest(toSign.getBytes(StandardCharsets.UTF_8)));

        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());

        String formBody = "file=" + URLEncoder.encode(dataUri, StandardCharsets.UTF_8)
                + "&api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&timestamp=" + timestamp
                + "&folder=" + URLEncoder.encode(safeFolder, StandardCharsets.UTF_8)
                + "&signature=" + signature;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
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

    private String saveLocally(MultipartFile file, String folder) {
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
            String filename = UUID.randomUUID() + ext;

            Path targetDir = Paths.get("uploads", folder).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            return "http://10.0.2.2:8080/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file ảnh: " + e.getMessage(), e);
        }
    }
}
