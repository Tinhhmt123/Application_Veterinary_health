package com.example.healthy_pet.ui.auth;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.healthy_pet.MainActivity;
import com.example.healthy_pet.R;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.User;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Hồ sơ cá nhân (gắn với profile_user.xml), hiển thị ở phần Content giữa của Page_User / Page_Admin.
 * Hỗ trợ chọn ảnh đại diện từ máy và upload lên Cloudinary qua @Multipart (PUT /api/users/{id}).
 */
public class AccountFragment extends Fragment {

    private View viewProfileBanner;
    private ImageView ivProfileAvatar;
    private TextView tvProfileEmail;
    private TextView tvProfileRoleBadge;
    private TextInputEditText etProfileFullName;
    private TextInputEditText etProfilePhone;
    private MaterialButton btnChangeAvatar;
    private MaterialButton btnSaveProfile;
    private MaterialButton btnLogout;
    private ProgressBar progressBarProfile;
    private SessionManager sessionManager;
    private Uri selectedAvatarUri = null;

    private final ActivityResultLauncher<String> pickAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && isAdded()) {
                    selectedAvatarUri = uri;
                    Glide.with(requireContext())
                            .load(uri)
                            .transform(new CircleCrop())
                            .placeholder(R.mipmap.ic_launcher_round)
                            .into(ivProfileAvatar);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sessionManager = new SessionManager(requireContext());

        if (!sessionManager.isLoggedIn()) {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openLoginPage();
            }
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.profile_user, container, false);

        viewProfileBanner = view.findViewById(R.id.viewProfileBanner);
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileRoleBadge = view.findViewById(R.id.tvProfileRoleBadge);
        etProfileFullName = view.findViewById(R.id.etProfileFullName);
        etProfilePhone = view.findViewById(R.id.etProfilePhone);
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBarProfile = view.findViewById(R.id.progressBarProfile);

        bindUserSessionToViews();
        refreshUserFromServer();

        // 1. Nút Đổi ảnh đại diện -> Chọn ảnh mới từ thư viện máy
        btnChangeAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));

        // 1b. Nhấp vào ảnh Avatar -> Hiện tag phóng to ảnh đại diện (không mở đổi hình)
        View.OnClickListener zoomAvatarListener = v -> showAvatarZoomDialog();
        ivProfileAvatar.setOnClickListener(zoomAvatarListener);
        View cardAvatarRing = view.findViewById(R.id.cardProfileAvatarRing);
        if (cardAvatarRing != null) {
            cardAvatarRing.setOnClickListener(zoomAvatarListener);
        }

        // 2. Lưu thay đổi Hồ sơ & Upload Avatar lên Cloudinary (@Multipart)
        btnSaveProfile.setOnClickListener(v -> handleSaveProfile());

        // 3. Đăng xuất tài khoản
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(getContext(), "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openLoginPage();
            }
        });

        return view;
    }

    private void handleSaveProfile() {
        String fullName = etProfileFullName.getText() != null ? etProfileFullName.getText().toString().trim() : "";
        String phone = etProfilePhone.getText() != null ? etProfilePhone.getText().toString().trim() : "";

        if (fullName.isEmpty()) {
            etProfileFullName.setError("Họ và tên không được để trống");
            etProfileFullName.requestFocus();
            return;
        }

        Long userId = sessionManager.getUserId();
        RequestBody fullNameBody = ImageMultipartUtils.toTextRequestBody(fullName);
        RequestBody phoneBody = ImageMultipartUtils.toTextRequestBody(phone);
        MultipartBody.Part avatarPart = null;

        if (selectedAvatarUri != null) {
            try {
                avatarPart = ImageMultipartUtils.uriToMultipart(requireContext(), selectedAvatarUri, "avatarFile");
            } catch (IOException e) {
                Toast.makeText(getContext(), "Không thể đọc file ảnh đã chọn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        setLoading(true);
        RetrofitClient.getAuthUserApi()
                .updateProfile(userId, fullNameBody, phoneBody, avatarPart)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                        if (!isAdded()) return;
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            selectedAvatarUri = null;
                            sessionManager.saveUser(response.body());
                            bindUserSessionToViews();
                            if (requireActivity() instanceof MainActivity) {
                                ((MainActivity) requireActivity()).syncDrawerUserHeader();
                            }
                            Toast.makeText(getContext(), "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Cập nhật thất bại (Mã lỗi: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        setLoading(false);
                        Toast.makeText(getContext(), "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bindUserSessionToViews() {
        etProfileFullName.setText(sessionManager.getFullName());
        tvProfileEmail.setText(sessionManager.getEmail());
        etProfilePhone.setText(sessionManager.getPhoneNumber());

        boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole());
        int primaryColor = ContextCompat.getColor(
                requireContext(), isAdmin ? R.color.indigo_600 : R.color.emerald_600);
        if (viewProfileBanner != null) {
            viewProfileBanner.setBackgroundColor(primaryColor);
        }
        btnSaveProfile.setBackgroundTintList(ColorStateList.valueOf(primaryColor));

        if (isAdmin) {
            tvProfileRoleBadge.setText("Quản trị viên");
            tvProfileRoleBadge.setBackgroundResource(R.drawable.bg_badge_indigo);
            tvProfileRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.indigo_600));
        } else {
            tvProfileRoleBadge.setText("Khách hàng");
            tvProfileRoleBadge.setBackgroundResource(R.drawable.bg_pet_weight_badge);
            tvProfileRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.emerald_700));
        }

        String avatarUrl = sessionManager.getAvatarUrl();
        if (selectedAvatarUri == null && avatarUrl != null && !avatarUrl.isEmpty()) {
            ImageMultipartUtils.loadCircularAvatar(requireContext(), avatarUrl, ivProfileAvatar);
        }
    }

    private void refreshUserFromServer() {
        Long userId = sessionManager.getUserId();
        RetrofitClient.getAuthUserApi().getUserById(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveUser(response.body());
                    bindUserSessionToViews();
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).syncDrawerUserHeader();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                // Giữ nguyên dữ liệu đang lưu trong SessionManager khi mất mạng tạm thời
            }
        });
    }

    /**
     * Hiện hộp thoại phóng to ảnh đại diện khi người dùng hoặc Admin nhấp vào Avatar.
     */
    private void showAvatarZoomDialog() {
        if (!isAdded()) return;

        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        int size = Math.round(280 * getResources().getDisplayMetrics().density);

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        container.setPadding(pad, pad, pad, pad);

        ImageView zoomedImageView = new ImageView(requireContext());
        android.widget.LinearLayout.LayoutParams imgParams =
                new android.widget.LinearLayout.LayoutParams(size, size);
        zoomedImageView.setLayoutParams(imgParams);
        zoomedImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        String avatarUrl = sessionManager.getAvatarUrl();
        if (selectedAvatarUri != null) {
            Glide.with(requireContext())
                    .load(selectedAvatarUri)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .into(zoomedImageView);
        } else if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Glide.with(requireContext())
                    .load(avatarUrl.trim())
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(zoomedImageView);
        } else {
            zoomedImageView.setImageResource(R.mipmap.ic_launcher_round);
        }

        container.addView(zoomedImageView);

        String titleName = sessionManager.getFullName();
        if (titleName == null || titleName.trim().isEmpty()) {
            titleName = "Ảnh đại diện";
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("🖼️ " + titleName)
                .setView(container)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void setLoading(boolean loading) {
        progressBarProfile.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!loading);
        btnChangeAvatar.setEnabled(!loading);
        btnLogout.setEnabled(!loading);
    }
}
