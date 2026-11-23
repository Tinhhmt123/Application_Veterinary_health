
package org.example.backend_app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Signed, expiring bearer tokens.
 * Set JWT_SECRET to a long random value in deployment.
 */
@Service
public class AccessTokenService {

    private final byte[] secret;
    private final long lifetimeSeconds;

    public AccessTokenService(
            @Value("${app.auth.token-secret:${JWT_SECRET:HealthyPet-DEV-ONLY-change-this-secret-before-deployment-2026}}")
            String secret,
            @Value("${app.auth.token-lifetime-seconds:86400}")
            long lifetimeSeconds) {

        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.lifetimeSeconds = lifetimeSeconds;

        if (this.secret.length < 32) {
            throw new IllegalArgumentException(
                    "Token secret must be at least 32 bytes"
            );
        }

        if (lifetimeSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Token lifetime must be greater than 0"
            );
        }
    }

    /**
     * Tạo Access Token cho người dùng.
     */
    public String issue(Long userId, String role) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Invalid user role");
        }

        long expiresAt = Instant.now().getEpochSecond()
                + lifetimeSeconds;

        String payload = userId
                + "|"
                + role.toUpperCase()
                + "|"
                + expiresAt;

        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        payload.getBytes(StandardCharsets.UTF_8)
                );

        return encoded + "." + sign(encoded);
    }

    /**
     * Xác minh Access Token.
     * Trả về null nếu token không hợp lệ hoặc hết hạn.
     */
    public AuthenticatedUser verify(String token) {

        try {
            if (token == null || token.isBlank()) {
                return null;
            }

            String[] parts = token.split("\\.", -1);

            if (parts.length != 2) {
                return null;
            }

            String expectedSignature = sign(parts[0]);

            boolean signatureValid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[1].getBytes(StandardCharsets.UTF_8)
            );

            if (!signatureValid) {
                return null;
            }

            String payload = new String(
                    Base64.getUrlDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8
            );

            String[] values = payload.split("\\|", -1);

            if (values.length != 3) {
                return null;
            }

            Long userId = Long.parseLong(values[0]);
            String role = values[1].toUpperCase();
            long expiresAt = Long.parseLong(values[2]);

            if (userId <= 0
                    || expiresAt <= Instant.now().getEpochSecond()) {
                return null;
            }

            if (!role.equals("USER") && !role.equals("ADMIN")) {
                return null;
            }

            return new AuthenticatedUser(userId, role);

        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Ký token bằng thuật toán HMAC-SHA256.
     */
    private String sign(String value) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(secret, "HmacSHA256")
            );

            byte[] signature = mac.doFinal(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signature);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot sign access token",
                    ex
            );
        }
    }

    /**
     * Thông tin người dùng được giải mã từ token.
     */
    public record AuthenticatedUser(Long id, String role) {
    }
}
