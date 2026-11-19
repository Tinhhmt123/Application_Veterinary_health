package com.example.healthy_pet.ui.component;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.healthy_pet.R;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Component hộp thoại dùng chung cho:
 * 1) Kích hoạt tài khoản bằng mã OTP 6 số gửi qua Email.
 * 2) Quên mật khẩu: Gửi mã OTP 6 số qua Email -> Nhập OTP + Mật khẩu mới.
 */
public class Auth_Otp_Dialog_User {

    public interface OnVerifiedListener {
        void onVerified(User verifiedUser);
    }

    public interface OnPasswordResetListener {
        void onPasswordResetSuccess(String email);
    }

    public static void showActivateDialog(
            @NonNull Context context,
            @NonNull String email,
            @NonNull OnVerifiedListener listener
    ) {
        View view = LayoutInflater.from(context).inflate(R.layout.otp_dialog_user, null, false);

        TextView tvTitle = view.findViewById(R.id.tvOtpDialogTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvOtpDialogSubtitle);
        ProgressBar progressBar = view.findViewById(R.id.progressBarOtpDialog);
        TextInputLayout tilEmail = view.findViewById(R.id.tilOtpEmail);
        TextInputEditText etEmail = view.findViewById(R.id.etOtpEmail);
        TextInputEditText etOtpCode = view.findViewById(R.id.etOtpCode);
        TextInputLayout tilNewPassword = view.findViewById(R.id.tilOtpNewPassword);
        MaterialButton btnPrimary = view.findViewById(R.id.btnOtpPrimaryAction);
        MaterialButton btnResend = view.findViewById(R.id.btnOtpSendOrResend);
        MaterialButton btnCancel = view.findViewById(R.id.btnOtpCancel);

        tvTitle.setText("Kích hoạt tài khoản");
        tvSubtitle.setText("Mã OTP 6 số đã được gửi tới email của bạn (hiệu lực 5 phút).");
        etEmail.setText(email);
        etEmail.setEnabled(false);
        tilEmail.setEnabled(false);
        tilNewPassword.setVisibility(View.GONE);
        btnPrimary.setText("Xác nhận kích hoạt");
        btnResend.setText("Gửi lại mã OTP");

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnResend.setOnClickListener(v -> {
            String targetEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            sendOtpRequest(context, targetEmail, "ACTIVATE", progressBar, btnPrimary, btnResend, null);
        });

        btnPrimary.setOnClickListener(v -> {
            String targetEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String otp = etOtpCode.getText() != null ? etOtpCode.getText().toString().trim() : "";
            if (otp.length() != 6) {
                etOtpCode.setError("Vui lòng nhập đủ 6 chữ số OTP");
                etOtpCode.requestFocus();
                return;
            }

            setDialogLoading(progressBar, btnPrimary, btnResend, true);
            Map<String, String> body = new HashMap<>();
            body.put("email", targetEmail);
            body.put("otp_code", otp);

            RetrofitClient.getAuthUserApi().verifyOtp(body).enqueue(new Callback<User>() {
                @Override
                public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                    setDialogLoading(progressBar, btnPrimary, btnResend, false);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(context, "Kích hoạt tài khoản thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        listener.onVerified(response.body());
                    } else if (response.code() == 403) {
                        Toast.makeText(context, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!", Toast.LENGTH_LONG).show();
                    } else {
                        etOtpCode.setError("Mã OTP không chính xác hoặc đã hết hạn!");
                        etOtpCode.requestFocus();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                    setDialogLoading(progressBar, btnPrimary, btnResend, false);
                    Toast.makeText(context, "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    public static void showForgotPasswordDialog(
            @NonNull Context context,
            String initialEmail,
            @NonNull OnPasswordResetListener listener
    ) {
        View view = LayoutInflater.from(context).inflate(R.layout.otp_dialog_user, null, false);

        TextView tvTitle = view.findViewById(R.id.tvOtpDialogTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvOtpDialogSubtitle);
        ProgressBar progressBar = view.findViewById(R.id.progressBarOtpDialog);
        TextInputEditText etEmail = view.findViewById(R.id.etOtpEmail);
        TextInputLayout tilOtpCode = view.findViewById(R.id.tilOtpCode);
        TextInputEditText etOtpCode = view.findViewById(R.id.etOtpCode);
        TextInputLayout tilNewPassword = view.findViewById(R.id.tilOtpNewPassword);
        TextInputEditText etNewPassword = view.findViewById(R.id.etOtpNewPassword);
        MaterialButton btnPrimary = view.findViewById(R.id.btnOtpPrimaryAction);
        MaterialButton btnSendOtp = view.findViewById(R.id.btnOtpSendOrResend);
        MaterialButton btnCancel = view.findViewById(R.id.btnOtpCancel);

        tvTitle.setText("Quên mật khẩu");
        tvSubtitle.setText("Nhập Email để nhận mã OTP 6 số và đặt lại mật khẩu mới.");
        if (initialEmail != null && !initialEmail.trim().isEmpty()) {
            etEmail.setText(initialEmail.trim());
        }
        tilOtpCode.setVisibility(View.VISIBLE);
        tilNewPassword.setVisibility(View.VISIBLE);
        btnPrimary.setText("Đặt lại mật khẩu");
        btnSendOtp.setText("Lấy mã OTP (6 số)");

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSendOtp.setOnClickListener(v -> {
            String targetEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (targetEmail.isEmpty()) {
                etEmail.setError("Vui lòng nhập Email của bạn");
                etEmail.requestFocus();
                return;
            }
            sendOtpRequest(
                    context,
                    targetEmail,
                    "RESET_PASSWORD",
                    progressBar,
                    btnPrimary,
                    btnSendOtp,
                    () -> {
                        btnSendOtp.setText("Gửi lại mã OTP");
                        etOtpCode.requestFocus();
                    }
            );
        });

        btnPrimary.setOnClickListener(v -> {
            String targetEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String otp = etOtpCode.getText() != null ? etOtpCode.getText().toString().trim() : "";
            String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";

            if (targetEmail.isEmpty()) {
                etEmail.setError("Vui lòng nhập Email");
                etEmail.requestFocus();
                return;
            }
            if (otp.length() != 6) {
                etOtpCode.setError("Vui lòng nhập mã OTP 6 chữ số (bấm 'Lấy mã OTP' nếu chưa có)");
                etOtpCode.requestFocus();
                return;
            }
            if (newPassword.length() < 6) {
                etNewPassword.setError("Mật khẩu mới phải từ 6 ký tự trở lên");
                etNewPassword.requestFocus();
                return;
            }

            setDialogLoading(progressBar, btnPrimary, btnSendOtp, true);
            Map<String, String> payload = new HashMap<>();
            payload.put("email", targetEmail);
            payload.put("otp_code", otp);
            payload.put("new_password", newPassword);

            RetrofitClient.getAuthUserApi().resetPassword(payload).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    setDialogLoading(progressBar, btnPrimary, btnSendOtp, false);
                    if (response.isSuccessful()) {
                        Toast.makeText(context, "Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        listener.onPasswordResetSuccess(targetEmail);
                    } else if (response.code() == 404) {
                        etEmail.setError("Email không tồn tại trong hệ thống!");
                        etEmail.requestFocus();
                    } else if (response.code() == 403) {
                        Toast.makeText(context, "Tài khoản đã bị khóa bởi Quản trị viên!", Toast.LENGTH_LONG).show();
                    } else {
                        etOtpCode.setError("Mã OTP không chính xác hoặc đã hết hạn!");
                        etOtpCode.requestFocus();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    setDialogLoading(progressBar, btnPrimary, btnSendOtp, false);
                    Toast.makeText(context, "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private static void sendOtpRequest(
            @NonNull Context context,
            @NonNull String email,
            @NonNull String purpose,
            @NonNull ProgressBar progressBar,
            @NonNull MaterialButton btnPrimary,
            @NonNull MaterialButton btnSendOtp,
            Runnable onSuccess
    ) {
        setDialogLoading(progressBar, btnPrimary, btnSendOtp, true);
        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("purpose", purpose);

        RetrofitClient.getAuthUserApi().sendOtp(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                setDialogLoading(progressBar, btnPrimary, btnSendOtp, false);
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Đã gửi mã OTP 6 số tới email " + email, Toast.LENGTH_LONG).show();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else if (response.code() == 404) {
                    Toast.makeText(context, "Email không tồn tại trong hệ thống!", Toast.LENGTH_LONG).show();
                } else if (response.code() == 403) {
                    Toast.makeText(context, "Tài khoản đã bị khóa bởi Quản trị viên!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Không thể gửi OTP (Mã lỗi: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                setDialogLoading(progressBar, btnPrimary, btnSendOtp, false);
                Toast.makeText(context, "Không thể kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void setDialogLoading(
            @NonNull ProgressBar progressBar,
            @NonNull MaterialButton btnPrimary,
            @NonNull MaterialButton btnSecondary,
            boolean loading
    ) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPrimary.setEnabled(!loading);
        btnSecondary.setEnabled(!loading);
    }
}
