
package com.example.healthy_pet.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiPetApi {

    // Gửi câu hỏi đến Spring Boot API
    @POST("ai/chat")
    Call<ChatResponse> sendMessage(
            @Body ChatRequest request
    );

    // Model dữ liệu gửi lên backend
    class ChatRequest {

        private String message;

        public ChatRequest(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    // Model dữ liệu nhận từ backend
    class ChatResponse {

        private String reply;

        public String getReply() {
            return reply;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }
    }
}
