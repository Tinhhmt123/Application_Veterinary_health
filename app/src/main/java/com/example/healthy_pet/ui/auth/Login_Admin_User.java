package com.example.healthy_pet.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.BuildConfig;
import com.example.healthy_pet.MainActivity;
import com.example.healthy_pet.R;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.User;
import com.example.healthy_pet.ui.component.Auth_Otp_Dialog_User;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Đăng nhập dùng chung cho cả Admin và User (gắn với login_admin_user.xml)
 * Hỗ trợ:
 * - Kiểm tra tài khoản bị khóa bởi Admin (403) vs chưa kích hoạt OTP 6 số (423 / is_verified == false).
 * - Quên mật khẩu qua mã OTP 6 số gửi về Email.
 * - Đăng nhập bằng Google (OAuth 2.0).
 */
public class Login_Admin_User extends Fragment {

    private TextInputEditText etLoginEmail;
    private TextInputEditText etLoginPassword;
    private MaterialButton btnLoginSubmit;
    private MaterialButton btnForgotPassword;
    private MaterialButton btnGoogleSignIn;
    private MaterialButton btnGoToRegister;
    private ProgressBar progressBarLogin;
    private SessionManager sessionManager;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_admin_user, container, false);

        sessionManager = new SessionManager(requireContext());
        etLoginEmail = view.findViewById(R.id.etLoginEmail);
        etLoginPassword = view.findViewById(R.id.etLoginPassword);
        btnLoginSubmit = view.findViewById(R.id.btnLoginSubmit);
        btnForgotPassword = view.findViewById(R.id.btnForgotPassword);
        btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn);
        btnGoToRegister = view.findViewById(R.id.btnGoToRegister);
        progressBarLogin = view.findViewById(R.id.progressBarLogin);

        setupGoogleSignInClient();

        btnLoginSubmit.setOnClickListener(v -> handleLogin());

        btnForgotPassword.setOnClickListener(v -> {
            String currentEmail = etLoginEmail.getText() != null ? etLoginEmail.getText().toString().trim() : "";
            Auth_Otp_Dialog_User.showForgotPasswordDialog(requireContext(), currentEmail, resetEmail -> {
                etLoginEmail.setText(resetEmail);
                etLoginPassword.setText("");
                etLoginPassword.requestFocus();
            });
        });

        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());

        btnGoToRegister.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new Register_User())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void setupGoogleSignInClient() {
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile();

        if (BuildConfig.GOOGLE_WEB_CLIENT_ID != null && !BuildConfig.GOOGLE_WEB_CLIENT_ID.trim().isEmpty()) {
            gsoBuilder.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID.trim());
        }
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gsoBuilder.build());
    }

    private void startGoogleSignIn() {
        if (googleSignInClient == null) {
            setupGoogleSignInClient();
        }
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null || account.getEmail() == null) {
                Toast.makeText(getContext(), "Không lấy được thông tin tài khoản Google!", Toast.LENGTH_SHORT).show();
                return;
            }
            setLoading(true);
            Map<String, String> payload = new HashMap<>();
            payload.put("email", account.getEmail());
            payload.put("full_name", account.getDisplayName() != null ? account.getDisplayName() : "");
            payload.put("avatar_url", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
            payload.put("id_token", account.getIdToken() != null ? account.getIdToken() : "");

            RetrofitClient.getAuthUserApi().googleLogin(payload).enqueue(new Callback<User>() {
                @Override
                public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                    if (!isAdded()) return;
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        onLoginSuccess(response.body());
                    } else if (response.code() == 403) {
                        Toast.makeText(getContext(), "Tài khoản của bạn đã bị khóa bởi Quản trị viên!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Đăng nhập Google thất bại (Mã lỗi: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    setLoading(false);
                    Toast.makeText(getContext(), "Không thể kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (ApiException e) {
            if (!isAdded()) return;
            Toast.makeText(
                    getContext(),
                    "Đăng nhập Google không thành công (Mã: " + e.getStatusCode() + "). Kiểm tra SHA-1 / Test Users trên Google Cloud Console.",
                    Toast.LENGTH_LONG
            ).show();
        }
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
                    if (Boolean.FALSE.equals(loggedInUser.getIsVerified())) {
                        Toast.makeText(
                                getContext(),
                                "Tài khoản chưa kích hoạt! Vui lòng nhập mã OTP 6 số gửi qua email.",
                                Toast.LENGTH_LONG
                        ).show();
                        Auth_Otp_Dialog_User.showActivateDialog(requireContext(), email, Login_Admin_User.this::onLoginSuccess);
                        return;
                    }
                    onLoginSuccess(loggedInUser);
                } else if (response.code() == 423) {
                    Toast.makeText(
                            getContext(),
                            "Tài khoản chưa kích hoạt! Mã OTP 6 số vừa được gửi tới email " + email,
                            Toast.LENGTH_LONG
                    ).show();
                    Auth_Otp_Dialog_User.showActivateDialog(requireContext(), email, Login_Admin_User.this::onLoginSuccess);
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
                Toast.makeText(
                        getContext(),
                        "Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng!",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void onLoginSuccess(@NonNull User loggedInUser) {
        if (!isAdded()) return;
        sessionManager.saveUser(loggedInUser);
        Toast.makeText(getContext(), "Xin chào, " + loggedInUser.getFullName() + "!", Toast.LENGTH_SHORT).show();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openRolePage(loggedInUser.getRole());
        }
    }

    private void setLoading(boolean loading) {
        progressBarLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLoginSubmit.setEnabled(!loading);
        btnForgotPassword.setEnabled(!loading);
        btnGoogleSignIn.setEnabled(!loading);
        btnGoToRegister.setEnabled(!loading);
    }
}
