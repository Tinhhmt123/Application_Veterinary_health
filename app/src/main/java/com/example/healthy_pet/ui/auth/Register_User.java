package com.example.healthy_pet.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.MainActivity;
import com.example.healthy_pet.R;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.User;
import com.example.healthy_pet.ui.component.Auth_Otp_Dialog_User;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Đăng ký tài khoản dành cho User (gắn với register_user.xml)
 */
public class Register_User extends Fragment {

    private TextInputEditText etRegisterFullName;
    private TextInputEditText etRegisterEmail;
    private TextInputEditText etRegisterPhone;
    private TextInputEditText etRegisterPassword;
    private MaterialButton btnRegisterSubmit;
    private MaterialButton btnBackToLogin;
    private ProgressBar progressBarRegister;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register_user, container, false);

        sessionManager = new SessionManager(requireContext());
        etRegisterFullName = view.findViewById(R.id.etRegisterFullName);
        etRegisterEmail = view.findViewById(R.id.etRegisterEmail);
        etRegisterPhone = view.findViewById(R.id.etRegisterPhone);
        etRegisterPassword = view.findViewById(R.id.etRegisterPassword);
        btnRegisterSubmit = view.findViewById(R.id.btnRegisterSubmit);
        btnBackToLogin = view.findViewById(R.id.btnBackToLogin);
        progressBarRegister = view.findViewById(R.id.progressBarRegister);

        btnRegisterSubmit.setOnClickListener(v -> handleRegister());

        btnBackToLogin.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openLoginPage();
            }
        });

        return view;
    }

    private void handleRegister() {
        String fullName = etRegisterFullName.getText() != null ? etRegisterFullName.getText().toString().trim() : "";
        String email = etRegisterEmail.getText() != null ? etRegisterEmail.getText().toString().trim() : "";
        String phone = etRegisterPhone.getText() != null ? etRegisterPhone.getText().toString().trim() : "";
        String password = etRegisterPassword.getText() != null ? etRegisterPassword.getText().toString() : "";

        if (fullName.isEmpty()) {
            etRegisterFullName.setError("Vui lòng nhập Họ và tên");
            etRegisterFullName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etRegisterEmail.setError("Vui lòng nhập Email");
            etRegisterEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegisterPassword.setError("Mật khẩu phải có từ 6 ký tự trở lên");
            etRegisterPassword.requestFocus();
            return;
        }

        setLoading(true);
        User registerRequest = new User(fullName, email, password, phone);

        RetrofitClient.getAuthUserApi().register(registerRequest).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (!isAdded()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    User newUser = response.body();
                    if (Boolean.FALSE.equals(newUser.getIsVerified())) {
                        Toast.makeText(
                                getContext(),
                                "Đăng ký thành công! Mã OTP 6 số đã được gửi tới email " + newUser.getEmail(),
                                Toast.LENGTH_LONG
                        ).show();
                        Auth_Otp_Dialog_User.showActivateDialog(requireContext(), newUser.getEmail(), verifiedUser -> {
                            if (!isAdded()) return;
                            sessionManager.saveUser(verifiedUser);
                            if (requireActivity() instanceof MainActivity) {
                                ((MainActivity) requireActivity()).openRolePage(verifiedUser.getRole());
                            }
                        });
                    } else {
                        sessionManager.saveUser(newUser);
                        Toast.makeText(getContext(), "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show();
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).openRolePage(newUser.getRole());
                        }
                    }
                } else if (response.code() == 409) {
                    etRegisterEmail.setError("Email này đã được đăng ký!");
                    etRegisterEmail.requestFocus();
                } else {
                    Toast.makeText(getContext(), "Đăng ký thất bại (Mã lỗi: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(getContext(), "Không thể kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBarRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegisterSubmit.setEnabled(!loading);
        btnBackToLogin.setEnabled(!loading);
    }
}
