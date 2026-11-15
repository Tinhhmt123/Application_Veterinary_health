
package com.example.healthy_pet.ui.ai;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthy_pet.R;

public class AiPetFragment extends Fragment {

    private LinearLayout messageContainer;
    private ScrollView scrollMessages;
    private EditText edtMessage;
    private Button btnSend;

    public AiPetFragment() {
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
                "Xin chào! Mình là AI Thú Y của HealthyPet 🐾\n\n" +
                        "Mình có thể hỗ trợ bạn tìm hiểu về dinh dưỡng, " +
                        "chăm sóc và sức khỏe thú cưng. Bạn cần giúp gì hôm nay?"
        );

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {

        String message = edtMessage.getText().toString().trim();

        if (message.isEmpty()) {
            edtMessage.setError("Vui lòng nhập câu hỏi");
            return;
        }

        addUserMessage(message);
        edtMessage.setText("");

        // Phản hồi tạm thời, chưa kết nối AI thật
        addBotMessage(
                "Cảm ơn bạn đã gửi câu hỏi! 🐾\n\n" +
                        "Chức năng AI đang trong quá trình tích hợp.\n\n" +
                        "Nếu thú cưng có biểu hiện nghiêm trọng hoặc bất thường, " +
                        "bạn nên đưa bé đến cơ sở thú y để được kiểm tra."
        );
    }

    private void addUserMessage(String message) {
        addMessage(message, true);
    }

    private void addBotMessage(String message) {
        addMessage(message, false);
    }

    private void addMessage(String message, boolean isUser) {

        TextView textView = new TextView(requireContext());

        textView.setText(message);
        textView.setTextSize(15);
        textView.setPadding(16, 12, 16, 12);

        if (isUser) {
            textView.setTextColor(
                    android.graphics.Color.WHITE
            );
        } else {
            textView.setTextColor(
                    android.graphics.Color.BLACK
            );
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(8, 8, 8, 8);

        if (isUser) {
            params.gravity = Gravity.END;
            textView.setBackgroundResource(
                    R.drawable.bg_ai_user_message
            );
        } else {
            params.gravity = Gravity.START;
            textView.setBackgroundResource(
                    R.drawable.bg_ai_bot_message
            );
        }

        textView.setMaxWidth(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.75)
        );

        messageContainer.addView(textView, params);

        scrollMessages.post(() ->
                scrollMessages.fullScroll(View.FOCUS_DOWN)
        );
    }
}
