package com.example.healthy_pet.ui.pet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.healthy_pet.R;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.api.VaccinationApi;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.models.VaccinationRecord;
import com.example.healthy_pet.models.WeightLog;
import com.example.healthy_pet.ui.Page_Admin;
import com.example.healthy_pet.ui.Page_User;
import com.example.healthy_pet.ui.component.Pet_Form_Admin_User;
import com.example.healthy_pet.ui.component.Weight_Logs_Admin_User;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Pet_Detail_Admin_User extends Fragment {

    private static final String ARG_PET = "arg_pet";
    private static final String ARG_IS_ADMIN = "arg_is_admin";

    private Pet currentPet;
    private boolean isAdminMode;

    private Pet_Form_Admin_User petFormAdminUser;

    private View viewBanner;
    private ImageView ivAvatar;

    private TextView tvNameSpecies;
    private TextView tvBreedGender;
    private TextView tvBirthDate;
    private TextView tvOwner;
    private TextView tvCurrentWeight;
    private TextView tvWeightTrend;

    private MaterialButton btnBooking;

    // Nhật ký cân nặng
    private ProgressBar progressBarLogs;
    private TextView tvLogsEmpty;
    private LinearLayout layoutTimeline;

    // Sổ tiêm chủng
    private ProgressBar progressVaccinations;
    private TextView tvVaccinationsEmpty;
    private TextView tvVaccinationCount;
    private LinearLayout layoutVaccinations;
    private MaterialButton btnAddVaccination;

    private VaccinationApi vaccinationApi;
    private SessionManager sessionManager;

    private boolean isSavingVaccination = false;

    // USER chọn ảnh dấu mộc cho một hồ sơ tiêm đã tồn tại.
    private Long pendingStampUploadRecordId;

    private final ActivityResultLauncher<String> pickVaccinationStampLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        Long recordId = pendingStampUploadRecordId;
                        pendingStampUploadRecordId = null;
                        if (uri != null && recordId != null) {
                            uploadVaccinationStamp(recordId, uri);
                        }
                    }
            );

    private final ActivityResultLauncher<String> pickPetAvatarLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (petFormAdminUser != null) {
                            petFormAdminUser.onImagePicked(uri);
                        }
                    }
            );

    public static Pet_Detail_Admin_User newInstance(
            @NonNull Pet pet,
            boolean isAdminMode
    ) {
        Pet_Detail_Admin_User fragment = new Pet_Detail_Admin_User();

        Bundle args = new Bundle();
        args.putSerializable(ARG_PET, pet);
        args.putBoolean(ARG_IS_ADMIN, isAdminMode);

        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.pet_detail_admin_user,
                container,
                false
        );

        if (getArguments() != null) {
            currentPet = (Pet) getArguments().getSerializable(ARG_PET);
            isAdminMode = getArguments().getBoolean(ARG_IS_ADMIN, false);
        }

        if (currentPet == null) {
            navigateBackToPetList();
            return view;
        }

        // =========================
        // ÁNH XẠ VIEW THÔNG TIN PET
        // =========================

        viewBanner = view.findViewById(R.id.viewDetailPetBanner);
        ivAvatar = view.findViewById(R.id.ivDetailPetAvatar);

        tvNameSpecies = view.findViewById(R.id.tvDetailPetNameSpecies);
        tvBreedGender = view.findViewById(R.id.tvDetailPetBreedGender);
        tvBirthDate = view.findViewById(R.id.tvDetailPetBirthDate);
        tvOwner = view.findViewById(R.id.tvDetailPetOwner);
        tvCurrentWeight = view.findViewById(R.id.tvDetailPetCurrentWeight);
        tvWeightTrend = view.findViewById(R.id.tvDetailWeightTrend);

        btnBooking = view.findViewById(R.id.btnDetailPetBooking);

        // =========================
        // ÁNH XẠ VIEW CÂN NẶNG
        // =========================

        progressBarLogs = view.findViewById(R.id.progressBarDetailLogs);
        tvLogsEmpty = view.findViewById(R.id.tvDetailLogsEmpty);
        layoutTimeline = view.findViewById(R.id.layoutDetailWeightTimeline);

        // =========================
        // ÁNH XẠ VIEW SỔ TIÊM
        // =========================

        progressVaccinations =
                view.findViewById(R.id.progressDetailVaccinations);

        tvVaccinationsEmpty =
                view.findViewById(R.id.tvDetailVaccinationsEmpty);

        tvVaccinationCount =
                view.findViewById(R.id.tvDetailVaccinationCount);

        layoutVaccinations =
                view.findViewById(R.id.layoutDetailVaccinations);

        btnAddVaccination =
                view.findViewById(R.id.btnDetailAddVaccination);

        sessionManager = new SessionManager(requireContext());
        vaccinationApi = RetrofitClient.getVaccinationApi();

        // Chỉ ADMIN mới thấy nút thêm
        btnAddVaccination.setVisibility(
                canManageVaccinations() ? View.VISIBLE : View.GONE
        );

        btnAddVaccination.setOnClickListener(
                v -> showVaccinationDialog(null)
        );

        // =========================
        // MÀU GIAO DIỆN THEO ROLE
        // =========================

        int roleColor = ContextCompat.getColor(
                requireContext(),
                isAdminMode ? R.color.indigo_600 : R.color.emerald_600
        );

        if (viewBanner != null) {
            viewBanner.setBackgroundColor(roleColor);
        }

        if (btnBooking != null) {
            btnBooking.setBackgroundTintList(
                    ColorStateList.valueOf(roleColor)
            );

            btnBooking.setOnClickListener(
                    v -> navigateToBookingPage()
            );
        }

        // =========================
        // FORM QUẢN LÝ THÚ CƯNG
        // =========================

        petFormAdminUser = new Pet_Form_Admin_User(
                this,
                isAdminMode,
                currentPet.getUserId(),
                pickPetAvatarLauncher,
                this::loadDetailAndLogs
        );

        view.findViewById(R.id.btnDetailPetEdit).setOnClickListener(
                v -> petFormAdminUser.showPetFormDialog(
                        currentPet,
                        this::loadDetailAndLogs
                )
        );

        view.findViewById(R.id.btnDetailPetWeight).setOnClickListener(
                v -> Weight_Logs_Admin_User.showWeightLogsDialog(
                        this,
                        currentPet,
                        this::loadDetailAndLogs
                )
        );

        view.findViewById(R.id.btnDetailPetDelete).setOnClickListener(
                v -> Pet_Form_Admin_User.confirmAndDeletePet(
                        this,
                        currentPet,
                        this::navigateBackToPetList
                )
        );

        // =========================
        // TẢI DỮ LIỆU
        // =========================

        bindPetHeader();
        loadDetailAndLogs();
        loadVaccinations();

        return view;
    }

    // =====================================================
    // PHÂN QUYỀN SỔ TIÊM CHỦNG
    // =====================================================

    private boolean canManageVaccinations() {
        return isAdded()
                && isAdminMode
                && sessionManager != null
                && "ADMIN".equalsIgnoreCase(sessionManager.getRole());
    }

    private boolean canUploadVaccinationStamp() {
        return isAdded()
                && !isAdminMode
                && sessionManager != null
                && "USER".equalsIgnoreCase(sessionManager.getRole());
    }

    // =====================================================
    // TẢI DANH SÁCH TIÊM CHỦNG
    // =====================================================

    private void loadVaccinations() {

        if (!isAdded()
                || currentPet == null
                || currentPet.getId() == null) {
            return;
        }

        progressVaccinations.setVisibility(View.VISIBLE);

        vaccinationApi.getVaccinationsByPet(
                currentPet.getId()
        ).enqueue(new Callback<List<VaccinationRecord>>() {

            @Override
            public void onResponse(
                    @NonNull Call<List<VaccinationRecord>> call,
                    @NonNull Response<List<VaccinationRecord>> response
            ) {

                if (!isAdded()) return;

                progressVaccinations.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {

                    renderVaccinations(response.body());

                } else {

                    Toast.makeText(
                            requireContext(),
                            "Không tải được sổ tiêm. Mã lỗi: " + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<VaccinationRecord>> call,
                    @NonNull Throwable t
            ) {

                if (!isAdded()) return;

                progressVaccinations.setVisibility(View.GONE);

                Toast.makeText(
                        requireContext(),
                        "Lỗi tải sổ tiêm: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // =====================================================
    // HIỂN THỊ DANH SÁCH TIÊM CHỦNG
    // =====================================================

    private void renderVaccinations(List<VaccinationRecord> records) {

        if (!isAdded()) return;

        layoutVaccinations.removeAllViews();

        if (records == null || records.isEmpty()) {

            tvVaccinationsEmpty.setVisibility(View.VISIBLE);
            tvVaccinationCount.setText("0 mũi tiêm");

            return;
        }

        tvVaccinationsEmpty.setVisibility(View.GONE);

        tvVaccinationCount.setText(
                records.size() + " mũi tiêm"
        );

        for (VaccinationRecord record : records) {

            LinearLayout card = new LinearLayout(requireContext());

            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable background = new GradientDrawable();

            background.setColor(
                    ContextCompat.getColor(
                            requireContext(),
                            R.color.slate_50
                    )
            );

            background.setCornerRadius(dp(10));

            card.setBackground(background);

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            cardParams.bottomMargin = dp(8);

            card.setLayoutParams(cardParams);

            // Tên vaccine
            TextView title = new TextView(requireContext());

            title.setText(
                    record.getVaccineName() == null
                            ? "Vaccine"
                            : record.getVaccineName()
            );

            title.setTextSize(14);
            title.setTypeface(null, Typeface.BOLD);

            title.setTextColor(
                    ContextCompat.getColor(
                            requireContext(),
                            R.color.slate_800
                    )
            );

            card.addView(title);

            // Ngày tiêm và ngày hẹn
            TextView dates = new TextView(requireContext());

            String detail =
                    "Ngày tiêm: " + safe(record.getInjectionDate())
                            + "\nMũi tiếp theo: " + safe(record.getNextDueDate());

            if (!TextUtils.isEmpty(record.getClinicName())) {
                detail += "\nPhòng khám: " + record.getClinicName();
            }

            dates.setText(detail);
            dates.setTextSize(12);

            dates.setTextColor(
                    ContextCompat.getColor(
                            requireContext(),
                            R.color.slate_500
                    )
            );

            card.addView(dates);

            // Chỉ ADMIN có nút Sửa và Xóa
            if (canManageVaccinations()) {

                LinearLayout actions = new LinearLayout(requireContext());
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setGravity(Gravity.CENTER_VERTICAL);
                actions.setPadding(dp(4), 0, dp(4), 0);

                MaterialButton btnEdit =
                        createDetailVaccinationButton("Sửa");

                MaterialButton btnDelete =
                        createDetailVaccinationButton("Xóa");

                // Hai nút chia đều chiều rộng, có khoảng cách thật 14dp.
                LinearLayout.LayoutParams editParams =
                        new LinearLayout.LayoutParams(
                                0,
                                dp(42),
                                1f
                        );

                LinearLayout.LayoutParams deleteParams =
                        new LinearLayout.LayoutParams(
                                0,
                                dp(42),
                                1f
                        );

                View gap = new View(requireContext());
                LinearLayout.LayoutParams gapParams =
                        new LinearLayout.LayoutParams(dp(14), 1);

                actions.addView(btnEdit, editParams);
                actions.addView(gap, gapParams);
                actions.addView(btnDelete, deleteParams);

                btnEdit.setOnClickListener(
                        v -> showVaccinationDialog(record)
                );

                btnDelete.setOnClickListener(
                        v -> confirmDeleteVaccination(record)
                );

                LinearLayout.LayoutParams actionsParams =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                actionsParams.topMargin = dp(10);

                card.addView(actions, actionsParams);
            } else if (canUploadVaccinationStamp()) {
                // USER chỉ được bổ sung/thay ảnh dấu mộc, không sửa dữ liệu tiêm.
                if (!TextUtils.isEmpty(record.getStampImageUrl())) {
                    TextView stampStatus = new TextView(requireContext());
                    stampStatus.setText("Đã có ảnh dấu mộc. Bạn có thể tải ảnh mới để thay thế.");
                    stampStatus.setTextSize(12);
                    stampStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.slate_600));
                    LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    statusParams.topMargin = dp(8);
                    card.addView(stampStatus, statusParams);
                }

                MaterialButton btnUploadStamp = new MaterialButton(requireContext());
                btnUploadStamp.setText(TextUtils.isEmpty(record.getStampImageUrl())
                        ? "📷 Tải ảnh dấu mộc"
                        : "📷 Thay ảnh dấu mộc");
                btnUploadStamp.setAllCaps(false);
                btnUploadStamp.setTextSize(13);
                btnUploadStamp.setCornerRadius(dp(10));
                btnUploadStamp.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
                btnUploadStamp.setTextColor(Color.WHITE);
                btnUploadStamp.setInsetTop(0);
                btnUploadStamp.setInsetBottom(0);
                btnUploadStamp.setMinHeight(dp(40));
                btnUploadStamp.setMinimumHeight(dp(40));

                LinearLayout.LayoutParams uploadParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(42)
                );
                uploadParams.topMargin = dp(10);
                card.addView(btnUploadStamp, uploadParams);
                btnUploadStamp.setOnClickListener(v -> {
                    if (record.getId() == null) {
                        Toast.makeText(requireContext(), "Không xác định được hồ sơ tiêm.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pendingStampUploadRecordId = record.getId();
                    pickVaccinationStampLauncher.launch("image/*");
                });
            }

            layoutVaccinations.addView(card);
        }
    }

    // =====================================================
    // TẠO NÚT SỬA / XÓA TRONG CHI TIẾT THÚ CƯNG
    // =====================================================

    private MaterialButton createDetailVaccinationButton(String text) {

        MaterialButton button = new MaterialButton(requireContext());

        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setCornerRadius(dp(10));
        button.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#059669"))
        );

        // Tương thích với MaterialButton: chỉ dùng inset trên/dưới.
        button.setInsetTop(0);
        button.setInsetBottom(0);

        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);

        return button;
    }

    // =====================================================
    // FORM THÊM / SỬA HỒ SƠ TIÊM
    // =====================================================

    private void showVaccinationDialog(
            @Nullable VaccinationRecord editing
    ) {

        if (!canManageVaccinations()) {
            Toast.makeText(
                    requireContext(),
                    "Chỉ ADMIN được quản lý hồ sơ tiêm.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(12));

        EditText vaccine = createInput("Tên vaccine");
        EditText injection = createInput("Ngày tiêm (yyyy-MM-dd)");
        EditText due = createInput("Ngày tiêm tiếp theo (yyyy-MM-dd)");
        EditText clinic = createInput("Tên phòng khám (không bắt buộc)");

        form.addView(vaccine);

        form.addView(injection);
        form.addView(due);
        form.addView(clinic);

        if (editing != null) {
            vaccine.setText(editing.getVaccineName());
            injection.setText(editing.getInjectionDate());
            due.setText(editing.getNextDueDate());
            clinic.setText(editing.getClinicName());

        }

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(editing == null ? "Thêm hồ sơ tiêm" : "Sửa hồ sơ tiêm")
                .setView(scrollView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            String name = vaccine.getText().toString().trim();
                            String date = injection.getText().toString().trim();
                            String next = due.getText().toString().trim();
                            String clinicName = clinic.getText().toString().trim();

                            if (name.isEmpty() || !isValidDate(date) || !isValidDate(next)) {
                                Toast.makeText(
                                        requireContext(),
                                        "Vui lòng nhập tên vaccine và ngày đúng yyyy-MM-dd.",
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    .format(new Date());

                            if (date.compareTo(today) > 0) {
                                Toast.makeText(
                                        requireContext(),
                                        "Ngày tiêm không được ở tương lai.",
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            if (next.compareTo(date) <= 0) {
                                Toast.makeText(
                                        requireContext(),
                                        "Ngày tiêm tiếp theo phải sau ngày tiêm hiện tại.",
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            saveVaccination(
                                    editing,
                                    name,
                                    date,
                                    next,
                                    clinicName,
                                    dialog
                            );
                        })
        );

        dialog.show();
    }

    // =====================================================
    // LƯU HỒ SƠ TIÊM
    // =====================================================

    private void saveVaccination(
            @Nullable VaccinationRecord editing,
            String name,
            String date,
            String next,
            String clinic,
            AlertDialog dialog
    ) {

        if (isSavingVaccination || !canManageVaccinations()) return;

        if (currentPet == null || currentPet.getId() == null) {
            Toast.makeText(requireContext(), "Không xác định được thú cưng.", Toast.LENGTH_SHORT).show();
            return;
        }

        isSavingVaccination = true;
        btnAddVaccination.setEnabled(false);

        RequestBody petId = createPart(String.valueOf(currentPet.getId()));
        RequestBody vaccineName = createPart(name);
        RequestBody injectionDate = createPart(date);
        RequestBody nextDueDate = createPart(next);
        RequestBody clinicName = createPart(clinic);

        Call<VaccinationRecord> call;
        if (editing == null) {
            call = vaccinationApi.createVaccination(
                    petId, vaccineName, injectionDate, nextDueDate, clinicName, null
            );
        } else {
            call = vaccinationApi.updateVaccination(
                    editing.getId(), petId, vaccineName, injectionDate,
                    nextDueDate, clinicName, null
            );
        }

        call.enqueue(new Callback<VaccinationRecord>() {
            @Override
            public void onResponse(
                    @NonNull Call<VaccinationRecord> call,
                    @NonNull Response<VaccinationRecord> response
            ) {
                if (!isAdded()) return;

                isSavingVaccination = false;
                if (btnAddVaccination != null) btnAddVaccination.setEnabled(true);

                if (response.isSuccessful()) {
                    dialog.dismiss();
                    loadVaccinations();
                    Toast.makeText(requireContext(), "Lưu hồ sơ tiêm thành công.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            requireContext(),
                            "Lưu hồ sơ thất bại. Mã lỗi: " + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<VaccinationRecord> call,
                    @NonNull Throwable t
            ) {
                if (!isAdded()) return;

                isSavingVaccination = false;
                if (btnAddVaccination != null) btnAddVaccination.setEnabled(true);
                Toast.makeText(
                        requireContext(),
                        "Lỗi lưu hồ sơ: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // USER chỉ cập nhật ảnh dấu mộc của hồ sơ thuộc thú cưng chính mình.
    private void uploadVaccinationStamp(Long recordId, Uri imageUri) {
        if (!isAdded()) return;
        if (!canUploadVaccinationStamp() || recordId == null || imageUri == null) {
            Toast.makeText(requireContext(), "Bạn không có quyền tải ảnh dấu mộc.", Toast.LENGTH_SHORT).show();
            return;
        }

        final MultipartBody.Part imagePart;
        try {
            imagePart = createStampImagePart(imageUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Không thể đọc ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        vaccinationApi.uploadVaccinationStamp(recordId, imagePart)
                .enqueue(new Callback<VaccinationRecord>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<VaccinationRecord> call,
                            @NonNull Response<VaccinationRecord> response
                    ) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Tải ảnh dấu mộc thành công.", Toast.LENGTH_SHORT).show();
                            loadVaccinations();
                        } else {
                            Toast.makeText(requireContext(), "Tải ảnh thất bại. Mã lỗi: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<VaccinationRecord> call,
                            @NonNull Throwable t
                    ) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Lỗi tải ảnh: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Đọc ảnh từ URI và đóng gói thành multipart có tên part stampImage
    private MultipartBody.Part createStampImagePart(Uri imageUri) throws Exception {
        String mimeType = requireContext().getContentResolver().getType(imageUri);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("Tệp được chọn không phải hình ảnh.");
        }

        byte[] imageBytes;
        try (InputStream input = requireContext().getContentResolver().openInputStream(imageUri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            if (input == null) throw new IllegalArgumentException("Không mở được ảnh.");

            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                if (output.size() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("Ảnh không được vượt quá 10 MB.");
                }
            }
            imageBytes = output.toByteArray();
        }

        if (imageBytes.length == 0) throw new IllegalArgumentException("Ảnh rỗng.");

        RequestBody body = RequestBody.create(MediaType.parse(mimeType), imageBytes);
        String extension = "jpg";
        if ("image/png".equalsIgnoreCase(mimeType)) extension = "png";
        else if ("image/webp".equalsIgnoreCase(mimeType)) extension = "webp";
        else if ("image/jpeg".equalsIgnoreCase(mimeType)) extension = "jpg";

        return MultipartBody.Part.createFormData(
                "stampImage",
                "vaccination_stamp." + extension,
                body
        );
    }

    // =====================================================
    // XÓA HỒ SƠ TIÊM
    // =====================================================

    private void confirmDeleteVaccination(
            VaccinationRecord record
    ) {

        if (!canManageVaccinations()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa hồ sơ tiêm chủng?")
                .setMessage("Bạn có chắc muốn xóa hồ sơ tiêm này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) ->
                        deleteVaccination(record)
                )
                .show();
    }

    private void deleteVaccination(VaccinationRecord record) {

        if (!canManageVaccinations()
                || record == null
                || record.getId() == null) {
            return;
        }

        vaccinationApi.deleteVaccination(
                record.getId()
        ).enqueue(new Callback<Void>() {

            @Override
            public void onResponse(
                    @NonNull Call<Void> call,
                    @NonNull Response<Void> response
            ) {

                if (!isAdded()) return;

                if (response.isSuccessful()) {

                    loadVaccinations();

                    Toast.makeText(
                            requireContext(),
                            "Đã xóa hồ sơ tiêm.",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                            requireContext(),
                            "Xóa thất bại. Mã lỗi: " + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<Void> call,
                    @NonNull Throwable t
            ) {

                if (!isAdded()) return;

                Toast.makeText(
                        requireContext(),
                        "Lỗi xóa hồ sơ: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // =====================================================
    // HÀM HỖ TRỢ SỔ TIÊM
    // =====================================================

    private EditText createInput(String hint) {

        EditText editText = new EditText(requireContext());

        editText.setSingleLine(true);
        editText.setHint(hint);

        return editText;
    }

    private RequestBody createPart(String value) {

        return RequestBody.create(
                MediaType.parse("text/plain"),
                value == null ? "" : value
        );
    }

    private boolean isValidDate(String value) {

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            format.setLenient(false);

            Date parsed = format.parse(value);

            return parsed != null;

        } catch (Exception e) {

            return false;
        }
    }

    private String safe(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Chưa cập nhật";
        }

        return value;
    }

    private int dp(float value) {

        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    // =====================================================
    // HIỂN THỊ THÔNG TIN THÚ CƯNG
    // =====================================================

    private void bindPetHeader() {

        if (!isAdded() || currentPet == null) return;

        Context context = requireContext();

        String species =
                currentPet.getSpecies() != null
                        && !currentPet.getSpecies().isEmpty()
                        ? currentPet.getSpecies()
                        : "Thú cưng";

        tvNameSpecies.setText(
                (currentPet.getName() != null
                        ? currentPet.getName()
                        : "Chưa đặt tên")
                        + " (" + species + ")"
        );

        String breed =
                currentPet.getBreed() != null
                        && !currentPet.getBreed().trim().isEmpty()
                        ? currentPet.getBreed().trim()
                        : "Chưa rõ giống";

        String gender =
                currentPet.getGender() != null
                        && !currentPet.getGender().trim().isEmpty()
                        ? currentPet.getGender().trim()
                        : "Chưa rõ";

        tvBreedGender.setText(
                "Giống: " + breed + " • " + gender
        );

        String birth =
                currentPet.getBirthDate() != null
                        && !currentPet.getBirthDate().trim().isEmpty()
                        ? currentPet.getBirthDate().trim()
                        : "Chưa cập nhật";

        tvBirthDate.setText("Ngày sinh: " + birth);

        double weight =
                currentPet.getCurrentWeight() != null
                        ? currentPet.getCurrentWeight()
                        : 0.0;

        tvCurrentWeight.setText(
                String.format(Locale.US, "%.1f kg", weight)
        );

        if (isAdminMode) {

            String ownerName =
                    currentPet.getOwnerName() != null
                            && !currentPet.getOwnerName().trim().isEmpty()
                            ? currentPet.getOwnerName().trim()
                            : "Khách #" + currentPet.getUserId();

            String ownerPhone =
                    currentPet.getOwnerPhone() != null
                            && !currentPet.getOwnerPhone().trim().isEmpty()
                            ? " • " + currentPet.getOwnerPhone().trim()
                            : "";

            tvOwner.setText(
                    "Chủ nuôi: " + ownerName + ownerPhone
            );

            tvOwner.setVisibility(View.VISIBLE);

        } else {

            tvOwner.setVisibility(View.GONE);
        }

        if (currentPet.getAvatarUrl() != null
                && !currentPet.getAvatarUrl().isEmpty()) {

            ImageMultipartUtils.loadCircularAvatar(
                    context,
                    currentPet.getAvatarUrl(),
                    ivAvatar
            );

        } else {

            ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    // =====================================================
    // TẢI THÔNG TIN VÀ NHẬT KÝ CÂN NẶNG
    // =====================================================

    private void loadDetailAndLogs() {

        if (currentPet == null || currentPet.getId() == null) {
            return;
        }

        Long petId = currentPet.getId();

        progressBarLogs.setVisibility(View.VISIBLE);

        RetrofitClient.getPetWeightApi()
                .getPetById(petId)
                .enqueue(new Callback<Pet>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<Pet> call,
                            @NonNull Response<Pet> response
                    ) {

                        if (!isAdded()
                                || !response.isSuccessful()
                                || response.body() == null) {
                            return;
                        }

                        currentPet = response.body();

                        bindPetHeader();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Pet> call,
                            @NonNull Throwable t
                    ) {
                        // Không làm gián đoạn màn hình chi tiết
                    }
                });

        RetrofitClient.getPetWeightApi()
                .getWeightLogsByPet(petId)
                .enqueue(new Callback<List<WeightLog>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<WeightLog>> call,
                            @NonNull Response<List<WeightLog>> response
                    ) {

                        if (!isAdded()) return;

                        progressBarLogs.setVisibility(View.GONE);

                        if (response.isSuccessful()
                                && response.body() != null) {

                            renderTimelineRows(response.body());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<WeightLog>> call,
                            @NonNull Throwable t
                    ) {

                        if (!isAdded()) return;

                        progressBarLogs.setVisibility(View.GONE);
                    }
                });
    }

    // =====================================================
    // HIỂN THỊ TIMELINE CÂN NẶNG
    // =====================================================

    private void renderTimelineRows(List<WeightLog> logs) {

        if (!isAdded()) return;

        Context context = requireContext();

        layoutTimeline.removeAllViews();

        if (logs == null || logs.isEmpty()) {

            tvLogsEmpty.setVisibility(View.VISIBLE);

            if (tvWeightTrend != null) {
                tvWeightTrend.setVisibility(View.GONE);
            }

            return;
        }

        tvLogsEmpty.setVisibility(View.GONE);

        updateWeightTrendSummary(context, logs);

        float density =
                context.getResources().getDisplayMetrics().density;

        int pad = Math.round(10 * density);
        int marginBottom = Math.round(8 * density);

        for (int i = 0; i < logs.size(); i++) {

            WeightLog log = logs.get(i);

            boolean isLatest = (i == 0);

            LinearLayout row = new LinearLayout(context);

            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.bottomMargin =
                    (i == logs.size() - 1) ? 0 : marginBottom;

            row.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();

            bg.setCornerRadius(12 * density);

            if (isLatest) {

                bg.setColor(
                        ContextCompat.getColor(
                                context,
                                R.color.emerald_50
                        )
                );

                bg.setStroke(
                        Math.round(1 * density),
                        ContextCompat.getColor(
                                context,
                                R.color.emerald_500
                        )
                );

            } else {

                bg.setColor(
                        ContextCompat.getColor(
                                context,
                                R.color.slate_100
                        )
                );
            }

            row.setBackground(bg);

            LinearLayout leftCol = new LinearLayout(context);

            leftCol.setOrientation(LinearLayout.VERTICAL);

            leftCol.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                    )
            );

            TextView tvDate = new TextView(context);

            String dateStr =
                    log.getRecordDate() != null
                            ? log.getRecordDate()
                            : "";

            tvDate.setText(
                    isLatest
                            ? dateStr + "  (Mới nhất)"
                            : dateStr
            );

            tvDate.setTextSize(12);
            tvDate.setTypeface(null, Typeface.BOLD);

            tvDate.setTextColor(
                    ContextCompat.getColor(
                            context,
                            isLatest
                                    ? R.color.emerald_700
                                    : R.color.slate_800
                    )
            );

            leftCol.addView(tvDate);

            if (log.getNote() != null
                    && !log.getNote().trim().isEmpty()) {

                TextView tvNote = new TextView(context);

                tvNote.setText(log.getNote().trim());
                tvNote.setTextSize(12);

                tvNote.setTextColor(
                        ContextCompat.getColor(
                                context,
                                R.color.slate_500
                        )
                );

                leftCol.addView(tvNote);
            }

            TextView tvWeight = new TextView(context);

            double weight =
                    log.getWeight() != null
                            ? log.getWeight()
                            : 0.0;

            tvWeight.setText(
                    String.format(Locale.US, "%.1f kg", weight)
            );

            tvWeight.setTextSize(14);
            tvWeight.setTypeface(null, Typeface.BOLD);

            tvWeight.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.emerald_700
                    )
            );

            row.addView(leftCol);
            row.addView(tvWeight);

            layoutTimeline.addView(row);
        }
    }

    // =====================================================
    // TÓM TẮT XU HƯỚNG CÂN NẶNG
    // =====================================================

    private void updateWeightTrendSummary(
            Context context,
            List<WeightLog> logs
    ) {

        if (tvWeightTrend == null
                || logs == null
                || logs.isEmpty()) {
            return;
        }

        double latest =
                logs.get(0).getWeight() != null
                        ? logs.get(0).getWeight()
                        : 0.0;

        double earliest =
                logs.get(logs.size() - 1).getWeight() != null
                        ? logs.get(logs.size() - 1).getWeight()
                        : 0.0;

        double diff = latest - earliest;

        tvWeightTrend.setVisibility(View.VISIBLE);

        if (logs.size() == 1) {

            tvWeightTrend.setText(
                    String.format(
                            Locale.US,
                            "⚖️ 1 mốc cân: %.1f kg",
                            latest
                    )
            );

            tvWeightTrend.setBackgroundResource(
                    R.drawable.bg_badge_slate
            );

            tvWeightTrend.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.slate_800
                    )
            );

        } else if (diff > 0.05) {

            tvWeightTrend.setText(
                    String.format(
                            Locale.US,
                            "📈 +%.1f kg so với lần đầu • %d mốc cân",
                            diff,
                            logs.size()
                    )
            );

            tvWeightTrend.setBackgroundResource(
                    R.drawable.bg_pet_weight_badge
            );

            tvWeightTrend.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.emerald_700
                    )
            );

        } else if (diff < -0.05) {

            tvWeightTrend.setText(
                    String.format(
                            Locale.US,
                            "📉 %.1f kg so với lần đầu • %d mốc cân",
                            diff,
                            logs.size()
                    )
            );

            tvWeightTrend.setBackgroundResource(
                    R.drawable.bg_badge_red
            );

            tvWeightTrend.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.red_600
                    )
            );

        } else {

            tvWeightTrend.setText(
                    String.format(
                            Locale.US,
                            "⚖️ Ổn định %.1f kg • %d mốc cân",
                            latest,
                            logs.size()
                    )
            );

            tvWeightTrend.setBackgroundResource(
                    R.drawable.bg_badge_indigo
            );

            tvWeightTrend.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.indigo_600
                    )
            );
        }
    }

    // =====================================================
    // ĐIỀU HƯỚNG
    // =====================================================

    private void navigateBackToPetList() {

        if (!isAdded()) return;

        Fragment parent = getParentFragment();

        if (parent != null
                && parent.getChildFragmentManager()
                .getBackStackEntryCount() > 0) {

            parent.getChildFragmentManager().popBackStack();
        }
    }

    private void navigateToBookingPage() {

        if (!isAdded()) return;

        Fragment parent = getParentFragment();

        if (parent instanceof Page_User) {

            ((Page_User) parent).navigateToBooking();

        } else if (parent instanceof Page_Admin) {

            ((Page_Admin) parent).navigateToAppointment();
        }
    }
}