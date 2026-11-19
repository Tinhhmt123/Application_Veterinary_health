
package com.example.healthy_pet.ui.ai;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthy_pet.R;
import com.example.healthy_pet.api.AiPetApi;
import com.example.healthy_pet.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiPetFragment extends Fragment {

    private LinearLayout messageContainer;
    private ScrollView scrollMessages;
    private EditText edtMessage;
    private Button btnSend;

    private boolean isSending = false;
    private TextView loadingMessage;

    public AiPetFragment() {
        // Constructor mặc định
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_ai_pet,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        messageContainer = view.findViewById(R.id.messageContainer);
        scrollMessages = view.findViewById(R.id.scrollMessages);
        edtMessage = view.findViewById(R.id.edtMessage);
        btnSend = view.findViewById(R.id.btnSend);

        addBotMessage(
                "Xin chào! 🐾 Mình là AI Thú Y. " +
                        "Bạn có thể hỏi mình về cách chăm sóc, dinh dưỡng " +
                        "và sức khỏe thú cưng nhé!"
        );

        btnSend.setOnClickListener(v -> sendMessage());

        edtMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {

        if (isSending) {
            return;
        }

        String message = edtMessage.getText().toString().trim();

        if (message.isEmpty()) {
            edtMessage.setError("Vui lòng nhập câu hỏi");
            return;
        }

        // Hiển thị tin nhắn người dùng
        addUserMessage(message);

        edtMessage.setText("");
        hideKeyboard();

        // Hiển thị trạng thái chờ
        loadingMessage = addBotMessage("AI Thú Y đang suy nghĩ... 🐾");

        isSending = true;
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.6f);

        AiPetApi.ChatRequest request =
                new AiPetApi.ChatRequest(message);

        RetrofitClient.getAiPetApi()
                .sendMessage(request)
                .enqueue(new Callback<AiPetApi.ChatResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<AiPetApi.ChatResponse> call,
                            @NonNull Response<AiPetApi.ChatResponse> response) {

                        if (!isAdded()) {
                            return;
                        }

                        removeLoadingMessage();

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getReply() != null
                                && !response.body().getReply().trim().isEmpty()) {

                            addBotMessage(response.body().getReply());

                        } else {
                            String errorMessage;

                            switch (response.code()) {
                                case 400:
                                    errorMessage = "Câu hỏi không hợp lệ. Vui lòng thử lại.";
                                    break;
                                case 404:
                                    errorMessage = "Không tìm thấy API AI trên máy chủ.";
                                    break;
                                case 502:
                                case 503:
                                case 504:
                                    errorMessage = "Dịch vụ AI hiện đang bận. Bạn vui lòng thử lại sau.";
                                    break;
                                default:
                                    errorMessage = "Không thể nhận phản hồi từ máy chủ. Mã lỗi: "
                                            + response.code();
                                    break;
                            }

                            addBotMessage(errorMessage);
                        }

                        finishSending();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<AiPetApi.ChatResponse> call,
                            @NonNull Throwable t) {

                        if (!isAdded()) {
                            return;
                        }

                        removeLoadingMessage();

                        addBotMessage(
                                "Không thể kết nối đến máy chủ AI. 🐾\n\n" +
                                        "Bạn hãy kiểm tra:\n" +
                                        "• Backend Spring Boot đã chạy chưa?\n" +
                                        "• Điện thoại và máy tính có cùng Wi-Fi không?\n" +
                                        "• Địa chỉ IP trong RetrofitClient có chính xác không?\n\n" +
                                        "Chi tiết: " + t.getMessage()
                        );

                        finishSending();
                    }
                });
    }

    private void finishSending() {
        isSending = false;

        if (btnSend != null) {
            btnSend.setEnabled(true);
            btnSend.setAlpha(1f);
        }
    }

    private TextView addUserMessage(String message) {
        return addMessage(message, true);
    }

    private TextView addBotMessage(String message) {
        return addMessage(message, false);
    }

    private TextView addMessage(String message, boolean isUser) {

        if (getContext() == null || messageContainer == null) {
            return null;
        }

        TextView textView = new TextView(requireContext());

        textView.setText(message);
        textView.setTextSize(15);
        textView.setTextColor(Color.parseColor("#263238"));
        textView.setPadding(32, 22, 32, 22);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(12, 10, 12, 10);

        if (isUser) {
            params.gravity = Gravity.END;
            params.setMarginStart(70);

            textView.setBackgroundColor(Color.parseColor("#D9F5E5"));
        } else {
            params.gravity = Gravity.START;
            params.setMarginEnd(70);

            textView.setBackgroundColor(Color.parseColor("#F0F2F5"));
        }

        textView.setLayoutParams(params);

        messageContainer.addView(textView);

        scrollToBottom();

        return textView;
    }

    private void removeLoadingMessage() {
        if (loadingMessage != null && messageContainer != null) {
            messageContainer.removeView(loadingMessage);
            loadingMessage = null;
        }
    }

    private void scrollToBottom() {
        if (scrollMessages != null) {
            scrollMessages.post(() ->
                    scrollMessages.fullScroll(View.FOCUS_DOWN)
            );
        }
    }

    private void hideKeyboard() {
        if (getContext() != null && edtMessage != null) {
            InputMethodManager imm =
                    (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(
                        edtMessage.getWindowToken(),
                        0
                );
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        messageContainer = null;
        scrollMessages = null;
        edtMessage = null;
        btnSend = null;
        loadingMessage = null;
    }
}
