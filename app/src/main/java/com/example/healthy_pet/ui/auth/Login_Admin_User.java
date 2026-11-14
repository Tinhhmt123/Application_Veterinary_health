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
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Đăng nhập dùng chung cho cả Admin và User (gắn với login_admin_user.xml)
 * Sau khi đăng nhập thành công:
 * - Nếu role == "ADMIN" -> Chuyển vào Page_Admin (page_admin.xml)
 * - Nếu role == "USER"  -> Chuyển vào Page_User (page_user.xml)
 */
public class Login_Admin_User extends Fragment {

    private TextInputEditText etLoginEmail;
    private TextInputEditText etLoginPassword;
    private MaterialButton btnLoginSubmit;
    private MaterialButton btnGoToRegister;
    private ProgressBar progressBarLogin;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_admin_user, container, false);

        sessionManager = new SessionManager(requireContext());
        etLoginEmail = view.findViewById(R.id.etLoginEmail);
        etLoginPassword = view.findViewById(R.id.etLoginPassword);
        btnLoginSubmit = view.findViewById(R.id.btnLoginSubmit);
        btnGoToRegister = view.findViewById(R.id.btnGoToRegister);
        progressBarLogin = view.findViewById(R.id.progressBarLogin);

        btnLoginSubmit.setOnClickListener(v -> handleLogin());

        btnGoToRegister.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new Register_User())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void handleLogin() {
        String email = etLoginEmail.getText() != null ? etLoginEmail.getText().toString().trim() : "";
        String password = etLoginPassword.getText() != null ? etLoginPassword.getText().toString() : "";

        if (email.isEmpty()) {
            etLoginEmail.setError("Vui lòng nhập Email");
            etLoginEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etLoginPassword.setError("Vui lòng nhập Mật khẩu");
            etLoginPassword.requestFocus();
            return;
        }

        setLoading(true);
        User loginRequest = new User(email, password);

        RetrofitClient.getAuthUserApi().login(loginRequest).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (!isAdded()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    User loggedInUser = response.body();
                    sessionManager.saveUser(loggedInUser);
                    Toast.makeText(getContext(), "Xin chào, " + loggedInUser.getFullName() + "!", Toast.LENGTH_SHORT).show();

                    // Kiểm tra role là ADMIN hay USER để chuyển vào page_admin hoặc page_user
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).openRolePage(loggedInUser.getRole());
                    }
                } else if (response.code() == 403) {
                    Toast.makeText(getContext(), "Tài khoản của bạn đã bị khóa bởi Quản trị viên!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Email hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show();
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
        progressBarLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLoginSubmit.setEnabled(!loading);
        btnGoToRegister.setEnabled(!loading);
    }
}
