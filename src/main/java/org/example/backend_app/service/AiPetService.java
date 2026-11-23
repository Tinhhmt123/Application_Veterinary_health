
package org.example.backend_app.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiPetService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private static final String SYSTEM_PROMPT =
            "Bạn là AI Thú Y của ứng dụng HealthyPet. " +
                    "Bạn giao tiếp bằng tiếng Việt, thân thiện, dễ hiểu. " +
                    "Bạn hỗ trợ người dùng về dinh dưỡng, chăm sóc, " +
                    "vệ sinh, hành vi và sức khỏe chó mèo. " +

                    "Khi trả lời, hãy trình bày rõ ràng, có thể sử dụng " +
                    "các gạch đầu dòng để người dùng dễ đọc. " +

                    "Không được khẳng định chắc chắn một chẩn đoán bệnh " +
                    "khi chưa có thăm khám trực tiếp. " +

                    "Không tự ý kê đơn thuốc hoặc hướng dẫn liều thuốc. " +

                    "Nếu thú cưng có dấu hiệu nguy hiểm như khó thở, " +
                    "co giật, chảy máu nhiều, ngộ độc hoặc bất tỉnh, " +
                    "hãy khuyên người dùng đưa thú cưng đến cơ sở thú y " +
                    "ngay lập tức. " +

                    "Nếu không đủ thông tin để tư vấn, hãy hỏi thêm " +
                    "về loài, tuổi, cân nặng và triệu chứng của thú cưng.";

    public AiPetService(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String chat(String message) {

        System.out.println("Working directory: "
                + System.getProperty("user.dir"));

        // Kiểm tra biến môi trường Windows / IntelliJ
        String envKey = System.getenv("GEMINI_API_KEY");

        System.out.println("OS Environment Key loaded: "
                + (envKey != null && !envKey.isBlank()));

        // Kiểm tra biến API Key Spring Boot nhận được
        System.out.println("Spring API Key loaded: "
                + (apiKey != null && !apiKey.isBlank()));

        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API Key chưa được cấu hình"
            );
        }


        // Giữ nguyên phần code phía dưới

        try {
            System.out.println("Gemini Model đang sử dụng: " + model);

            String endpoint =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model
                            + ":generateContent";

            Map<String, Object> requestBody = Map.of(

                    "system_instruction",
                    Map.of(
                            "parts",
                            List.of(
                                    Map.of("text", SYSTEM_PROMPT)
                            )
                    ),

                    "contents",
                    List.of(
                            Map.of(
                                    "role", "user",
                                    "parts",
                                    List.of(
                                            Map.of("text", message)
                                    )
                            )
                    ),

                    "generationConfig",
                    Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 800
                    )
            );

            String jsonBody =
                    objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(
                            HttpRequest.BodyPublishers.ofString(jsonBody)
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                System.err.println("Gemini HTTP Status: "
                        + response.statusCode());

                System.err.println("Gemini Error Body: "
                        + response.body());

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini API trả về lỗi HTTP "
                                + response.statusCode()
                                + ". Chi tiết: "
                                + response.body()
                );
            }

            JsonNode root =
                    objectMapper.readTree(response.body());

            JsonNode textNode = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode()
                    || textNode.isNull()
                    || textNode.asText().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini không trả về nội dung câu trả lời"
                );
            }

            return textNode.asText();

        } catch (ResponseStatusException e) {

            throw e;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Yêu cầu Gemini bị gián đoạn"
            );

        } catch (Exception e) {
            System.err.println("LỖI XỬ LÝ PHẢN HỒI GEMINI:");
            e.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể xử lý phản hồi từ Gemini: " + e.getMessage(),
                    e
            );
        }
    }
}
