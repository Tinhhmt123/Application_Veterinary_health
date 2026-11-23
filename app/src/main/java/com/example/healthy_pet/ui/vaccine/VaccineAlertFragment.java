package com.example.healthy_pet.ui.vaccine;
import android.app.AlertDialog;
import android.util.Log;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthy_pet.R;
import com.example.healthy_pet.api.PetWeightApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.api.VaccinationApi;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.models.VaccinationRecord;
import com.example.healthy_pet.utils.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.bumptech.glide.Glide;

public class VaccineAlertFragment extends Fragment {

    private static final String ARG_HISTORY_ONLY = "history_only";
    private boolean historyOnly = false;

    private Spinner spinnerPets;

    private TextView tvTotalVaccinations;
    private TextView tvUpcomingVaccinations;
    private TextView tvAlertEmpty;
    private TextView tvHistoryEmpty;
    private TextView tvError;

    private LinearLayout layoutAlerts;
    private LinearLayout layoutVaccinationHistory;

    private Button btnAddVaccination;
    private Button btnVaccinationNotifications;
    private ProgressBar progressBar;

    // Danh sách cảnh báo được lưu để hiển thị trong hộp thoại khi bấm chuông.
    private final List<VaccinationRecord> currentHealthAlerts = new ArrayList<>();

    private PetWeightApi petWeightApi;
    private VaccinationApi vaccinationApi;

    private SessionManager sessionManager;

    private Long userId;
    private Long selectedPetId;

    private final List<Pet> petList = new ArrayList<>();

    private Call<List<Pet>> petsCall;
    private Call<List<VaccinationRecord>> historyCall;
    private Call<List<VaccinationRecord>> alertsCall;
    private Call<VaccinationRecord> createCall;

    private boolean isCreatingVaccination = false;

    // Chỉ USER sử dụng launcher này để tải ảnh dấu mộc cho hồ sơ đã tồn tại.
    private ActivityResultLauncher<String> stampUploadLauncher;
    private Long pendingStampRecordId;

    /** Hiển thị ảnh dấu mộc đã được lưu trong hồ sơ tiêm chủng. */
    private void addStampImage(LinearLayout parent, String imageUrl) {
        if (parent == null || !isAdded()) {
            return;
        }

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Log.w("VACCINE_IMAGE", "Không có URL ảnh dấu mộc.");
            return;
        }

        String url = imageUrl.trim();

        // Ghép URL tương đối với địa chỉ máy chủ Backend.
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            String serverRoot = RetrofitClient.BASE_URL
                    .replaceFirst("/api/?$", "")
                    .replaceAll("/$", "");

            if (!url.startsWith("/")) {
                url = "/" + url;
            }

            url = serverRoot + url;
        }

        Log.d("VACCINE_IMAGE", "URL ảnh cuối cùng: " + url);

        ImageView imageView = new ImageView(requireContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(180)
        );

        params.setMargins(
                dpToPx(10), dpToPx(8),
                dpToPx(10), dpToPx(16)
        );

        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.WHITE);

        parent.addView(imageView);

        Glide.with(this)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView);
    }

    public VaccineAlertFragment() {
    }

    /** Tạo màn hình chỉ hiển thị lịch sử tiêm chủng, mở từ menu trượt. */
    public static VaccineAlertFragment newHistoryInstance() {
        VaccineAlertFragment fragment = new VaccineAlertFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HISTORY_ONLY, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stampUploadLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    Long recordId = pendingStampRecordId;
                    pendingStampRecordId = null;

                    if (uri != null && recordId != null) {
                        uploadStampForUser(recordId, uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_vaccine_alert,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        historyOnly = getArguments() != null
                && getArguments().getBoolean(ARG_HISTORY_ONLY, false);

        initViews(view);
        if (historyOnly) {
            configureHistoryOnlyScreen(view);
        } else {
            configureMainVaccineScreen(view);
        }

        sessionManager = new SessionManager(requireContext());

        petWeightApi = RetrofitClient.getPetWeightApi();
        vaccinationApi = RetrofitClient.getVaccinationApi();

        userId = sessionManager.getUserId();

        // USER chỉ được xem sổ tiêm; ADMIN mới được thao tác thêm hồ sơ.
        btnAddVaccination.setVisibility(isAdmin() ? View.VISIBLE : View.GONE);
        btnAddVaccination.setEnabled(false);

        btnAddVaccination.setOnClickListener(v -> {
            if (!"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
                showToast("Tài khoản USER chỉ có quyền xem sổ tiêm chủng.");
                return;
            }

            if (userId == null || selectedPetId == null) {
                showToast("Vui lòng chọn thú cưng trước!");
                return;
            }

            showAddVaccinationDialog();
        });

        if (userId == null || userId <= 0) {

            showError(
                    "Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại."
            );

            return;
        }

        loadPets();
    }

    private void initViews(View view) {

        spinnerPets = view.findViewById(R.id.spinnerPets);

        tvTotalVaccinations =
                view.findViewById(R.id.tvTotalVaccinations);

        tvUpcomingVaccinations =
                view.findViewById(R.id.tvUpcomingVaccinations);

        tvAlertEmpty =
                view.findViewById(R.id.tvAlertEmpty);

        tvHistoryEmpty =
                view.findViewById(R.id.tvHistoryEmpty);

        tvError = view.findViewById(R.id.tvError);

        layoutAlerts =
                view.findViewById(R.id.layoutAlerts);

        layoutVaccinationHistory =
                view.findViewById(R.id.layoutVaccinationHistory);

        btnAddVaccination =
                view.findViewById(R.id.btnAddVaccination);

        progressBar =
                view.findViewById(R.id.progressBar);

        if (!historyOnly) {
            setupAlertNotificationHeader(view);
        }
    }

    /**
     * Màn hình Sổ tiêm chủng chính chỉ hiển thị bộ chọn thú cưng,
     * thống kê và thanh thông báo. Danh sách lịch sử chỉ mở từ menu.
     */
    private void configureMainVaccineScreen(View root) {
        // Ẩn toàn bộ khu vực lịch sử ở màn hình chính.
        hideSectionAtContentLevel(root, layoutVaccinationHistory);
        hideSectionAtContentLevel(root, tvHistoryEmpty);
        hideHistoryHeading(root);

        // Đảm bảo phần cảnh báo cũ dạng danh sách không chiếm chỗ.
        if (layoutAlerts != null) layoutAlerts.setVisibility(View.GONE);
        if (tvAlertEmpty != null) tvAlertEmpty.setVisibility(View.GONE);
    }

    private void hideHistoryHeading(View view) {
        if (view instanceof TextView) {
            CharSequence value = ((TextView) view).getText();
            if (value != null) {
                String text = value.toString().toLowerCase(Locale.ROOT);
                if (text.contains("lịch sử tiêm chủng")
                        || text.contains("lịch sử tiêm phòng")) {
                    view.setVisibility(View.GONE);
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                hideHistoryHeading(group.getChildAt(i));
            }
        }
    }

    /** Chỉ giữ bộ chọn thú cưng và danh sách hồ sơ khi mở từ menu Lịch sử. */
    private void configureHistoryOnlyScreen(View root) {
        if (layoutAlerts != null) layoutAlerts.setVisibility(View.GONE);
        if (tvAlertEmpty != null) tvAlertEmpty.setVisibility(View.GONE);

        // Ẩn các khối thống kê và tiêu đề cảnh báo, không đụng đến bộ chọn thú cưng
        // hoặc danh sách lịch sử tiêm chủng.
        hideSectionAtContentLevel(root, tvTotalVaccinations);
        hideSectionAtContentLevel(root, tvUpcomingVaccinations);
        hideSectionAtContentLevel(root, layoutAlerts);

        // Ẩn tiêu đề trang cũ và đổi thành tiêu đề riêng của màn hình lịch sử.
        updateHistoryHeading(root);

        View content = findScrollContent(root);
        if (content instanceof ViewGroup) {
            TextView heading = new TextView(requireContext());
            heading.setText("📋 Lịch sử tiêm chủng");
            heading.setTextColor(Color.parseColor("#1E293B"));
            heading.setTextSize(22);
            heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            heading.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(8));
            ((ViewGroup) content).addView(heading, 0);
        }
    }

    private View findScrollContent(View root) {
        if (root instanceof ScrollView) {
            ScrollView scroll = (ScrollView) root;
            return scroll.getChildCount() > 0 ? scroll.getChildAt(0) : root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findScrollContent(group.getChildAt(i));
                if (found != null && found != group.getChildAt(i)) return found;
                if (group.getChildAt(i) instanceof ScrollView) {
                    ScrollView scroll = (ScrollView) group.getChildAt(i);
                    return scroll.getChildCount() > 0 ? scroll.getChildAt(0) : scroll;
                }
            }
        }
        return root;
    }

    private void hideSectionAtContentLevel(View root, View target) {
        if (target == null) return;
        View content = findScrollContent(root);
        View current = target;
        while (current.getParent() instanceof View && current.getParent() != content) {
            current = (View) current.getParent();
        }
        if (current != content) current.setVisibility(View.GONE);
        else target.setVisibility(View.GONE);
    }

    private void updateHistoryHeading(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CharSequence value = textView.getText();
            if (value != null) {
                String text = value.toString().toLowerCase(Locale.ROOT);
                if (text.contains("cảnh báo tiêm phòng")
                        || text.contains("cảnh báo tiêm chủng")) {
                    textView.setVisibility(View.GONE);
                } else if (text.contains("lịch sử tiêm chủng")
                        || text.equals("💉 sổ tiêm chủng")
                        || text.equals("sổ tiêm chủng")) {
                    textView.setVisibility(View.GONE);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateHistoryHeading(group.getChildAt(i));
            }
        }
    }

    /** Tiêu đề cảnh báo nằm cùng một hàng với nút chuông thông báo. */
    private void setupAlertNotificationHeader(View root) {
        if (layoutAlerts == null) {
            return;
        }

        // Ẩn tiêu đề cũ trong XML để tránh hiển thị trùng.
        hideOldAlertHeading(root);

        // Đặt thanh thông báo ở đầu nội dung fragment, phía trên tiêu đề,
        // bộ chọn thú cưng và các thẻ thống kê.
        View contentView = findScrollContent(root);
        if (!(contentView instanceof ViewGroup)) {
            return;
        }
        ViewGroup content = (ViewGroup) contentView;

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dpToPx(2), dpToPx(7), dpToPx(2), dpToPx(7));

        TextView title = new TextView(requireContext());
        title.setText("💉 Cảnh báo tiêm phòng");
        title.setTextColor(Color.parseColor("#334155"));
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        btnVaccinationNotifications = new Button(requireContext());
        btnVaccinationNotifications.setAllCaps(false);
        btnVaccinationNotifications.setText("🔔 0");
        btnVaccinationNotifications.setTextColor(Color.WHITE);
        btnVaccinationNotifications.setTextSize(14);
        btnVaccinationNotifications.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btnVaccinationNotifications.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        btnVaccinationNotifications.setMinWidth(dpToPx(64));
        btnVaccinationNotifications.setMinHeight(dpToPx(42));
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor("#059669"));
        buttonBackground.setCornerRadius(dpToPx(22));
        btnVaccinationNotifications.setBackground(buttonBackground);
        btnVaccinationNotifications.setOnClickListener(v -> showVaccinationNotifications());
        header.addView(btnVaccinationNotifications);

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(10));
        content.addView(header, 0, headerParams);

        // Ẩn các thẻ cảnh báo dài khỏi màn hình chính.
        layoutAlerts.setVisibility(View.GONE);
        if (tvAlertEmpty != null) tvAlertEmpty.setVisibility(View.GONE);
    }

    private void hideOldAlertHeading(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                String value = text.toString().toLowerCase(Locale.ROOT);
                if (value.contains("cảnh báo tiêm phòng")
                        || value.contains("cảnh báo tiêm chủng")) {
                    view.setVisibility(View.GONE);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                hideOldAlertHeading(group.getChildAt(i));
            }
        }
    }

    /** Mở danh sách chi tiết các mũi tiêm khi người dùng nhấn nút chuông. */
    private void showVaccinationNotifications() {
        if (!isAdded()) return;

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(4));
        scrollView.addView(content);

        if (currentHealthAlerts.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Hiện không có mũi tiêm quá hạn hoặc sắp đến hạn.");
            empty.setTextColor(Color.parseColor("#475569"));
            empty.setTextSize(15);
            empty.setPadding(dpToPx(8), dpToPx(18), dpToPx(8), dpToPx(18));
            content.addView(empty);
        } else {
            for (VaccinationRecord record : currentHealthAlerts) {
                String dueDate = record.getNextDueDate();
                String information =
                        "⚠️ Vaccine: " + safeText(record.getVaccineName())
                                + "\n📅 Ngày tiêm tiếp theo: " + safeText(dueDate)
                                + "\n🏥 Phòng khám: " + safeText(record.getClinicName())
                                + "\n📌 Trạng thái: " + getVaccinationStatus(dueDate);
                addInfoCard(content, information, dueDate);
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Thông báo tiêm phòng")
                .setView(scrollView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private boolean isAdmin() {
        return sessionManager != null
                && "ADMIN".equalsIgnoreCase(sessionManager.getRole());
    }

    private boolean isUser() {
        return sessionManager != null
                && "USER".equalsIgnoreCase(sessionManager.getRole());
    }

    private void loadPets() {

        if (userId == null || !isAdded()) {
            return;
        }

        showLoading(true);
        hideError();

        spinnerPets.setEnabled(false);
        btnAddVaccination.setEnabled(false);

        cancelCall(petsCall);

        petsCall = isAdmin()
                ? petWeightApi.searchAllPets("", "", null)
                : petWeightApi.getPetsByUser(userId);

        petsCall.enqueue(new Callback<List<Pet>>() {

            @Override
            public void onResponse(
                    Call<List<Pet>> call,
                    Response<List<Pet>> response
            ) {

                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                showLoading(false);
                spinnerPets.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {

                    petList.clear();
                    petList.addAll(response.body());

                    if (petList.isEmpty()) {

                        selectedPetId = null;

                        btnAddVaccination.setEnabled(false);
                        spinnerPets.setEnabled(false);

                        clearVaccinationData();

                        showError("Bạn chưa có thú cưng nào.");

                        return;
                    }

                    setupPetSpinner();

                } else {

                    selectedPetId = null;

                    btnAddVaccination.setEnabled(false);

                    clearVaccinationData();

                    showError(
                            "Không thể tải danh sách thú cưng. Mã lỗi: "
                                    + response.code()
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<List<Pet>> call,
                    Throwable t
            ) {

                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                showLoading(false);

                spinnerPets.setEnabled(false);
                btnAddVaccination.setEnabled(false);

                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void setupPetSpinner() {

        if (!isAdded() || petList.isEmpty()) {
            return;
        }

        List<String> petNames = new ArrayList<>();

        for (Pet pet : petList) {

            String name = pet.getName();
            String species = pet.getSpecies();

            if (name == null || name.trim().isEmpty()) {
                name = "Thú cưng";
            }

            if (species == null || species.trim().isEmpty()) {
                species = "Chưa xác định";
            }

            petNames.add(name + " - " + species);
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        petNames
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPets.setAdapter(adapter);

        spinnerPets.setEnabled(true);
        btnAddVaccination.setEnabled(
                isAdmin()
        );

        spinnerPets.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position < 0 || position >= petList.size()) {
                            return;
                        }

                        Pet selectedPet = petList.get(position);

                        Long newPetId = selectedPet.getId();

                        if (newPetId == null || newPetId <= 0) {
                            showError("ID thú cưng không hợp lệ.");
                            return;
                        }

                        if (newPetId.equals(selectedPetId)) {
                            return;
                        }

                        selectedPetId = newPetId;

                        hideError();

                        cancelCall(historyCall);
                        cancelCall(alertsCall);

                        clearVaccinationData();

                        // Luôn tải lịch sử theo petId để cập nhật Tổng mũi tiêm.
                        loadVaccinationHistory();

                        // Màn hình chính mới cần tải cảnh báo và số sắp đến hạn.
                        if (!historyOnly) {
                            loadHealthAlerts();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

                        selectedPetId = null;

                        cancelCall(historyCall);
                        cancelCall(alertsCall);

                        clearVaccinationData();

                        btnAddVaccination.setEnabled(false);
                    }
                }
        );

        spinnerPets.setSelection(0);
    }

    private void loadVaccinationHistory() {

        if (selectedPetId == null || userId == null || !isAdded()) {
            return;
        }

        final Long requestedPetId = selectedPetId;

        cancelCall(historyCall);

        historyCall = vaccinationApi.getVaccinationsByPet(requestedPetId);

        historyCall.enqueue(new Callback<List<VaccinationRecord>>() {

            @Override
            public void onResponse(
                    Call<List<VaccinationRecord>> call,
                    Response<List<VaccinationRecord>> response
            ) {

                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                if (!requestedPetId.equals(selectedPetId)) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {

                    displayVaccinationHistory(response.body());

                } else {

                    layoutVaccinationHistory.removeAllViews();

                    tvTotalVaccinations.setText("0");

                    tvHistoryEmpty.setText(
                            "Không thể tải lịch sử tiêm chủng. Mã lỗi: "
                                    + response.code()
                    );

                    tvHistoryEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(
                    Call<List<VaccinationRecord>> call,
                    Throwable t
            ) {

                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                if (!requestedPetId.equals(selectedPetId)) {
                    return;
                }

                tvHistoryEmpty.setText(
                        "Lỗi tải lịch sử: " + t.getMessage()
                );

                tvHistoryEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayVaccinationHistory(
            List<VaccinationRecord> records
    ) {
        layoutVaccinationHistory.removeAllViews();

        tvTotalVaccinations.setText(String.valueOf(records.size()));

        if (records.isEmpty()) {
            tvHistoryEmpty.setText("Chưa có lịch sử tiêm chủng.");
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvHistoryEmpty.setVisibility(View.GONE);

        for (VaccinationRecord record : records) {

            String nextDueDate = record.getNextDueDate();

            String information =
                    "💉 Vaccine: " + safeText(record.getVaccineName())
                            + "\n📅 Ngày tiêm: "
                            + safeText(record.getInjectionDate())
                            + "\n⏰ Ngày tiêm tiếp theo: "
                            + safeText(nextDueDate)
                            + "\n🏥 Phòng khám: "
                            + safeText(record.getClinicName())
                            + "\n📌 Trạng thái: "
                            + getVaccinationStatus(nextDueDate);

            // Hiển thị thông tin hồ sơ
            addInfoCard(
                    layoutVaccinationHistory,
                    information,
                    nextDueDate
            );

            // Hiển thị ảnh dấu mộc từ Backend
            addStampImage(
                    layoutVaccinationHistory,
                    record.getStampImageUrl()
            );

            if (isAdmin()) {
                // ADMIN chỉ quản lý thông tin hồ sơ tiêm chủng.
                addAdminRecordActions(layoutVaccinationHistory, record);
            } else if (isUser()) {
                // USER chỉ được tải/thay ảnh dấu mộc của hồ sơ đã tồn tại.
                addUserStampUploadButton(layoutVaccinationHistory, record);
            }
        }
    }

    /** Tạo nút tải/thay ảnh dấu mộc dành riêng cho USER. */
    private void addUserStampUploadButton(
            LinearLayout parent,
            VaccinationRecord record
    ) {
        if (!isAdded() || parent == null || record == null
                || record.getId() == null || !isUser()) {
            return;
        }

        boolean hasStamp = !TextUtils.isEmpty(record.getStampImageUrl());

        com.google.android.material.button.MaterialButton button =
                new com.google.android.material.button.MaterialButton(requireContext());

        button.setText(hasStamp
                ? "📷 Thay ảnh dấu mộc"
                : "📷 Tải ảnh dấu mộc");
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setCornerRadius(dpToPx(10));
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#059669"))
        );
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(44)
        );
        params.setMargins(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(14));
        parent.addView(button, params);

        button.setOnClickListener(v -> {
            if (!isUser()) {
                showToast("Bạn không có quyền tải ảnh dấu mộc.");
                return;
            }

            pendingStampRecordId = record.getId();
            stampUploadLauncher.launch("image/*");
        });
    }

    /** Gửi riêng ảnh dấu mộc; không gửi các trường thông tin vaccine. */
    private void uploadStampForUser(Long recordId, Uri imageUri) {
        if (!isAdded() || !isUser()) {
            showToast("Bạn không có quyền tải ảnh dấu mộc.");
            return;
        }

        if (recordId == null || imageUri == null) {
            showToast("Không tìm thấy hồ sơ hoặc ảnh được chọn.");
            return;
        }

        MultipartBody.Part stampImagePart;
        try {
            stampImagePart = createStampImagePart(imageUri);
        } catch (Exception e) {
            showToast("Không thể đọc ảnh dấu mộc: " + e.getMessage());
            return;
        }

        showLoading(true);

        vaccinationApi.uploadVaccinationStamp(recordId, stampImagePart)
                .enqueue(new Callback<VaccinationRecord>() {
                    @Override
                    public void onResponse(
                            Call<VaccinationRecord> call,
                            Response<VaccinationRecord> response
                    ) {
                        if (!isAdded() || call.isCanceled()) {
                            return;
                        }

                        showLoading(false);

                        if (response.isSuccessful()) {
                            showToast("Tải ảnh dấu mộc thành công.");
                            loadVaccinationHistory();
                            loadHealthAlerts();
                        } else {
                            String message = "Không thể tải ảnh dấu mộc. Mã lỗi: "
                                    + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    message += "\n" + response.errorBody().string();
                                }
                            } catch (Exception ignored) {
                            }
                            showError(message);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<VaccinationRecord> call,
                            Throwable t
                    ) {
                        if (!isAdded() || call.isCanceled()) {
                            return;
                        }
                        showLoading(false);
                        showError("Lỗi tải ảnh dấu mộc: " + t.getMessage());
                    }
                });
    }

    private void addAdminRecordActions(
            LinearLayout parent,
            VaccinationRecord record
    ) {
        if (!isAdded() || parent == null || record == null) {
            return;
        }

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(
                dpToPx(12),
                0,
                dpToPx(12),
                dpToPx(12)
        );

        int greenColor = Color.parseColor("#059669");

        com.google.android.material.button.MaterialButton edit =
                createVaccinationActionButton("Sửa", greenColor);

        com.google.android.material.button.MaterialButton delete =
                createVaccinationActionButton("Xóa", greenColor);

        // Hai nút có chiều rộng bằng nhau.
        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(
                        0,
                        dpToPx(42),
                        1f
                );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        0,
                        dpToPx(42),
                        1f
                );

        // Dùng Space để tạo khoảng trống cố định, không phụ thuộc margin của Button.
        Space gap = new Space(requireContext());
        LinearLayout.LayoutParams gapParams =
                new LinearLayout.LayoutParams(
                        dpToPx(14),
                        1
                );

        actions.addView(edit, editParams);
        actions.addView(gap, gapParams);
        actions.addView(delete, deleteParams);

        edit.setOnClickListener(v -> showVaccinationDialog(record));

        delete.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xóa hồ sơ tiêm chủng")
                        .setMessage("Bạn có chắc chắn muốn xóa hồ sơ này không?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton(
                                "Xóa",
                                (dialog, which) -> deleteVaccination(record)
                        )
                        .show()
        );

        parent.addView(
                actions,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private com.google.android.material.button.MaterialButton
    createVaccinationActionButton(String label, int backgroundColor) {

        com.google.android.material.button.MaterialButton button =
                new com.google.android.material.button.MaterialButton(requireContext());

        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setCornerRadius(dpToPx(11));
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(backgroundColor)
        );

        // Chỉ dùng các API được hỗ trợ; không gọi setInsetLeft/Right.
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setPadding(
                dpToPx(8),
                0,
                dpToPx(8),
                0
        );

        return button;
    }

    private void deleteVaccination(VaccinationRecord record) {
        if (!isAdmin() || record.getId() == null) {
            showToast("Bạn không có quyền xóa hồ sơ.");
            return;
        }
        vaccinationApi.deleteVaccination(record.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    showToast("Đã xóa hồ sơ tiêm chủng.");
                    loadVaccinationHistory();
                    loadHealthAlerts();
                } else showError("Không thể xóa hồ sơ. Mã lỗi: " + response.code());
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                if (isAdded()) showError("Lỗi xóa hồ sơ: " + t.getMessage());
            }
        });
    }

    public void refreshHealthAlerts() {

        if (!isAdded()) {
            return;
        }

        if (selectedPetId == null) {
            return;
        }

        loadHealthAlerts();
    }

    private void loadHealthAlerts() {

        if (historyOnly || !isAdded() || selectedPetId == null) {
            return;
        }

        // Nếu người dùng tắt cảnh báo 7 ngày, xóa dữ liệu cũ của thú cưng.
        if (sessionManager != null
                && !sessionManager.isAlert7DaysEnabled()) {

            cancelCall(alertsCall);
            currentHealthAlerts.clear();

            if (btnVaccinationNotifications != null) {
                btnVaccinationNotifications.setText("🔔 0");
            }
            if (tvUpcomingVaccinations != null) {
                tvUpcomingVaccinations.setText("0");
            }
            if (layoutAlerts != null) {
                layoutAlerts.removeAllViews();
                layoutAlerts.setVisibility(View.GONE);
            }
            if (tvAlertEmpty != null) {
                tvAlertEmpty.setText("Bạn đã tắt cảnh báo tiêm chủng.");
                tvAlertEmpty.setVisibility(View.GONE);
            }
            return;
        }

        if (userId == null) {
            return;
        }

        // Ghi nhớ petId của request để bỏ qua phản hồi cũ khi người dùng đổi thú cưng.
        final Long requestedPetId = selectedPetId;
        cancelCall(alertsCall);

        // Theo VaccinationApi hiện tại: getHealthAlerts() không nhận tham số.
        alertsCall = vaccinationApi.getHealthAlerts();

        alertsCall.enqueue(new Callback<List<VaccinationRecord>>() {

            @Override
            public void onResponse(
                    Call<List<VaccinationRecord>> call,
                    Response<List<VaccinationRecord>> response
            ) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                // Không cập nhật UI nếu người dùng đã chuyển sang thú cưng khác.
                if (!requestedPetId.equals(selectedPetId)) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<VaccinationRecord> petAlerts = new ArrayList<>();

                    for (VaccinationRecord record : response.body()) {
                        if (record != null
                                && record.getPetId() != null
                                && requestedPetId.equals(record.getPetId())) {
                            petAlerts.add(record);
                        }
                    }

                    displayHealthAlerts(petAlerts);
                } else {
                    currentHealthAlerts.clear();
                    layoutAlerts.removeAllViews();
                    layoutAlerts.setVisibility(View.GONE);
                    tvUpcomingVaccinations.setText("0");

                    if (btnVaccinationNotifications != null) {
                        btnVaccinationNotifications.setText("🔔 0");
                    }
                    if (tvAlertEmpty != null) {
                        tvAlertEmpty.setText(
                                "Không thể tải cảnh báo tiêm chủng. Mã lỗi: "
                                        + response.code()
                        );
                        tvAlertEmpty.setVisibility(View.GONE);
                    }
                    showToast("Không thể tải cảnh báo tiêm chủng. Mã lỗi: "
                            + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<VaccinationRecord>> call,
                    Throwable t
            ) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }
                if (!requestedPetId.equals(selectedPetId)) {
                    return;
                }

                currentHealthAlerts.clear();
                if (btnVaccinationNotifications != null) {
                    btnVaccinationNotifications.setText("🔔 0");
                }
                if (tvUpcomingVaccinations != null) {
                    tvUpcomingVaccinations.setText("0");
                }
                if (tvAlertEmpty != null) {
                    tvAlertEmpty.setVisibility(View.GONE);
                }
                showToast("Lỗi tải cảnh báo: " + t.getMessage());
            }
        });
    }

    private void displayHealthAlerts(List<VaccinationRecord> alerts) {
        if (!isAdded()) {
            return;
        }

        layoutAlerts.removeAllViews();
        currentHealthAlerts.clear();
        if (alerts != null) {
            currentHealthAlerts.addAll(alerts);
        }

        // Nút chuông đếm tất cả cảnh báo của thú cưng đang chọn.
        if (btnVaccinationNotifications != null) {
            btnVaccinationNotifications.setText(
                    "🔔 " + currentHealthAlerts.size()
            );
        }

        // Thẻ "Sắp đến hạn" chỉ đếm các mũi trong 7 ngày tới,
        // không cộng các mũi đã quá hạn.
        int upcomingCount = 0;
        for (VaccinationRecord record : currentHealthAlerts) {
            if (record != null
                    && "Sắp đến hạn".equals(
                    getVaccinationStatus(record.getNextDueDate()))) {
                upcomingCount++;
            }
        }

        if (tvUpcomingVaccinations != null) {
            tvUpcomingVaccinations.setText(String.valueOf(upcomingCount));
        }

        // Danh sách chi tiết chỉ mở trong hộp thoại khi nhấn nút chuông.
        layoutAlerts.setVisibility(View.GONE);
        if (tvAlertEmpty != null) {
            tvAlertEmpty.setVisibility(View.GONE);
        }
    }

    private void showAddVaccinationDialog() {
        showVaccinationDialog(null);
    }

    private void showVaccinationDialog(@Nullable VaccinationRecord editing) {

        if (!isAdded() || selectedPetId == null) {
            return;
        }

        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);

        int padding = dpToPx(20);

        form.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        EditText edtVaccineName =
                createEditText("Tên vaccine");

        EditText edtInjectionDate =
                createEditText("Ngày tiêm (yyyy-MM-dd)");
        attachDatePicker(edtInjectionDate);

        EditText edtNextDueDate =
                createEditText("Ngày tiêm tiếp theo (yyyy-MM-dd)");
        attachDatePicker(edtNextDueDate);

        EditText edtClinicName =
                createEditText("Tên phòng khám");

        // ADMIN chỉ nhập thông tin tiêm chủng; không tải ảnh dấu mộc tại đây.
        form.addView(edtVaccineName);
        form.addView(edtInjectionDate);
        form.addView(edtNextDueDate);
        form.addView(edtClinicName);

        // Điền dữ liệu cũ khi ADMIN chỉnh sửa hồ sơ.
        if (editing != null) {
            edtVaccineName.setText(editing.getVaccineName());
            edtInjectionDate.setText(editing.getInjectionDate());
            edtNextDueDate.setText(editing.getNextDueDate());
            edtClinicName.setText(editing.getClinicName());
        }

        // Cho phép cuộn biểu mẫu trên màn hình nhỏ.
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.addView(form);

        AlertDialog dialog =
                new AlertDialog.Builder(requireContext())
                        .setTitle(editing == null
                                ? "Thêm hồ sơ tiêm chủng"
                                : "Cập nhật hồ sơ tiêm chủng")
                        .setView(scrollView)
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Lưu", null)
                        .create();

        dialog.setOnShowListener(d -> {

            Button saveButton =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            saveButton.setOnClickListener(v -> {

                String vaccineName =
                        edtVaccineName.getText().toString().trim();

                String injectionDate =
                        edtInjectionDate.getText().toString().trim();

                String nextDueDate =
                        edtNextDueDate.getText().toString().trim();

                String clinicName =
                        edtClinicName.getText().toString().trim();

                if (TextUtils.isEmpty(vaccineName)
                        || TextUtils.isEmpty(injectionDate)
                        || TextUtils.isEmpty(nextDueDate)) {

                    showToast(
                            "Vui lòng nhập đầy đủ thông tin bắt buộc."
                    );

                    return;
                }

                if (!isValidDate(injectionDate)
                        || !isValidDate(nextDueDate)) {

                    showToast(
                            "Ngày phải đúng định dạng yyyy-MM-dd."
                    );

                    return;
                }

                String today = new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                ).format(new Date());

                if (injectionDate.compareTo(today) > 0) {

                    showToast(
                            "Ngày tiêm không được lớn hơn ngày hiện tại!"
                    );

                    return;
                }

                if (nextDueDate.compareTo(injectionDate) <= 0) {

                    showToast(
                            "Ngày tiêm tiếp theo phải sau ngày tiêm."
                    );

                    return;
                }

                createVaccination(
                        editing,
                        vaccineName,
                        injectionDate,
                        nextDueDate,
                        clinicName,
                        dialog
                );
            });
        });

        dialog.show();
    }

    private EditText createEditText(String hint) {

        EditText editText = new EditText(requireContext());

        editText.setHint(hint);
        editText.setSingleLine(true);

        return editText;
    }

    private void attachDatePicker(EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(v -> {
            if (!isAdded()) return;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            String currentText = editText.getText().toString().trim();
            if (isValidDate(currentText)) {
                try {
                    Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(currentText);
                    if (parsed != null) {
                        cal.setTime(parsed);
                    }
                } catch (Exception ignored) {
                }
            }
            new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> editText.setText(
                            String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    ),
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH),
                    cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void createVaccination(
            @Nullable VaccinationRecord editing,
            String vaccineName,
            String injectionDate,
            String nextDueDate,
            String clinicName,
            AlertDialog dialog
    ) {

        if (userId == null || selectedPetId == null) {

            showError(
                    "Không tìm thấy người dùng hoặc thú cưng."
            );

            return;
        }

        if (isCreatingVaccination) {
            return;
        }

        isCreatingVaccination = true;

        btnAddVaccination.setEnabled(false);

        RequestBody petIdBody =
                createPartFromString(
                        String.valueOf(selectedPetId)
                );

        RequestBody vaccineNameBody =
                createPartFromString(vaccineName);

        RequestBody injectionDateBody =
                createPartFromString(injectionDate);

        RequestBody nextDueDateBody =
                createPartFromString(nextDueDate);

        RequestBody clinicNameBody =
                createPartFromString(clinicName);

        // ADMIN không gửi ảnh dấu mộc khi thêm hoặc cập nhật hồ sơ.
        MultipartBody.Part stampImagePart = null;

        showLoading(true);

        final Long requestedPetId = selectedPetId;

        createCall = editing == null
                ? vaccinationApi.createVaccination(
                petIdBody, vaccineNameBody, injectionDateBody,
                nextDueDateBody, clinicNameBody, stampImagePart)
                : vaccinationApi.updateVaccination(
                editing.getId(), petIdBody, vaccineNameBody, injectionDateBody,
                nextDueDateBody, clinicNameBody, stampImagePart);

        createCall.enqueue(new Callback<VaccinationRecord>() {

            @Override
            public void onResponse(
                    Call<VaccinationRecord> call,
                    Response<VaccinationRecord> response
            ) {

                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                showLoading(false);

                isCreatingVaccination = false;

                btnAddVaccination.setEnabled(selectedPetId != null && isAdmin());

                if (response.isSuccessful() && response.body() != null) {

                    dialog.dismiss();

                    showToast(editing == null
                            ? "Thêm hồ sơ tiêm chủng thành công!"
                            : "Cập nhật hồ sơ tiêm chủng thành công!");

                    if (requestedPetId.equals(selectedPetId)) {

                        loadVaccinationHistory();
                        loadHealthAlerts();
                    }

                } else {

                    String errorMessage =
                            "Không thể thêm hồ sơ. Mã lỗi: "
                                    + response.code();

                    try {

                        if (response.errorBody() != null) {

                            errorMessage += "\n"
                                    + response.errorBody().string();
                        }

                    } catch (Exception ignored) {
                    }

                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(
                    Call<VaccinationRecord> call,
                    Throwable t
            ) {

                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                showLoading(false);

                isCreatingVaccination = false;

                btnAddVaccination.setEnabled(selectedPetId != null && isAdmin());

                showError(
                        "Lỗi kết nối khi thêm hồ sơ: "
                                + t.getMessage()
                );
            }
        });
    }

    private MultipartBody.Part createStampImagePart(Uri imageUri)
            throws Exception {

        if (!isAdded()) {
            throw new Exception("Fragment không còn hoạt động.");
        }

        String mimeType =
                requireContext()
                        .getContentResolver()
                        .getType(imageUri);

        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new Exception("Tệp được chọn không phải hình ảnh.");
        }

        byte[] imageBytes;

        try (InputStream inputStream =
                     requireContext()
                             .getContentResolver()
                             .openInputStream(imageUri);
             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            if (inputStream == null) {
                throw new Exception("Không thể mở ảnh.");
            }

            byte[] buffer = new byte[4096];

            int length;

            while ((length = inputStream.read(buffer)) != -1) {

                outputStream.write(buffer, 0, length);

                if (outputStream.size() > 10 * 1024 * 1024) {
                    throw new Exception("Ảnh không được vượt quá 10MB.");
                }
            }

            imageBytes = outputStream.toByteArray();
        }

        if (imageBytes.length == 0) {
            throw new Exception("Ảnh không có dữ liệu.");
        }

        RequestBody requestBody =
                RequestBody.create(
                        MediaType.parse(mimeType),
                        imageBytes
                );

        String fileName =
                "vaccination_stamp." + extensionForMime(mimeType);

        return MultipartBody.Part.createFormData(
                "stampImage",
                fileName,
                requestBody
        );
    }

    private String extensionForMime(String mimeType) {

        if (mimeType == null) {
            return "jpg";
        }

        switch (mimeType.toLowerCase(Locale.US)) {

            case "image/png":
                return "png";

            case "image/webp":
                return "webp";

            case "image/jpeg":
                return "jpg";

            default:
                return "jpg";
        }
    }

    private RequestBody createPartFromString(String value) {

        return RequestBody.create(
                MediaType.parse("text/plain"),
                value == null ? "" : value
        );
    }

    private String getVaccinationStatus(String nextDueDate) {

        if (!isValidDate(nextDueDate)) {
            return "Chưa xác định";
        }

        try {

            SimpleDateFormat formatter =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            formatter.setLenient(false);

            Date dueDate = formatter.parse(nextDueDate);

            if (dueDate == null) {
                return "Chưa xác định";
            }

            java.util.Calendar todayCalendar =
                    java.util.Calendar.getInstance();

            todayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayCalendar.set(java.util.Calendar.MINUTE, 0);
            todayCalendar.set(java.util.Calendar.SECOND, 0);
            todayCalendar.set(java.util.Calendar.MILLISECOND, 0);

            Date today = todayCalendar.getTime();

            java.util.Calendar limitCalendar =
                    (java.util.Calendar) todayCalendar.clone();

            limitCalendar.add(java.util.Calendar.DAY_OF_YEAR, 7);

            Date sevenDaysLater = limitCalendar.getTime();

            if (dueDate.before(today)) {
                return "Đã quá hạn";
            }

            if (!dueDate.after(sevenDaysLater)) {
                return "Sắp đến hạn";
            }

            return "Chưa đến hạn";

        } catch (Exception e) {

            return "Chưa xác định";
        }
    }

    private void addInfoCard(
            LinearLayout parent,
            String information,
            String nextDueDate
    ) {

        TextView textView = new TextView(requireContext());

        textView.setText(information);
        textView.setTextSize(14);
        textView.setTextColor(Color.parseColor("#334155"));
        textView.setLineSpacing(dpToPx(2), 1.0f);

        textView.setPadding(
                dpToPx(14),
                dpToPx(12),
                dpToPx(14),
                dpToPx(12)
        );

        int backgroundColor;
        int borderColor;

        switch (getVaccinationStatus(nextDueDate)) {

            case "Đã quá hạn":

                backgroundColor = Color.parseColor("#FEE2E2");
                borderColor = Color.parseColor("#DC2626");

                break;

            case "Sắp đến hạn":

                backgroundColor = Color.parseColor("#FFEDD5");
                borderColor = Color.parseColor("#EA580C");

                break;

            case "Chưa đến hạn":

                backgroundColor = Color.parseColor("#DCFCE7");
                borderColor = Color.parseColor("#16A34A");

                break;

            default:

                backgroundColor = Color.parseColor("#F1F5F9");
                borderColor = Color.parseColor("#94A3B8");

                break;
        }

        GradientDrawable background = new GradientDrawable();

        background.setColor(backgroundColor);
        background.setCornerRadius(dpToPx(12));
        background.setStroke(dpToPx(2), borderColor);

        textView.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, dpToPx(12));

        parent.addView(textView, params);
    }

    private String safeText(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Chưa cập nhật";
        }

        return value;
    }

    private boolean isValidDate(String date) {

        if (date == null
                || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {

            return false;
        }

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        format.setLenient(false);

        ParsePosition position = new ParsePosition(0);

        Date parsedDate = format.parse(date, position);

        return parsedDate != null
                && position.getIndex() == date.length();
    }

    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void showLoading(boolean isLoading) {

        if (progressBar != null) {

            progressBar.setVisibility(
                    isLoading ? View.VISIBLE : View.GONE
            );
        }
    }

    private void showError(String message) {

        if (tvError != null) {

            tvError.setText(message);

            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {

        if (tvError != null) {
            tvError.setVisibility(View.GONE);
        }
    }

    private void showToast(String message) {

        if (isAdded()) {

            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void clearVaccinationData() {

        currentHealthAlerts.clear();
        if (btnVaccinationNotifications != null) {
            btnVaccinationNotifications.setText("🔔 0");
        }
        if (layoutAlerts != null) {
            layoutAlerts.removeAllViews();
            layoutAlerts.setVisibility(View.GONE);
        }

        if (layoutVaccinationHistory != null) {
            layoutVaccinationHistory.removeAllViews();
        }

        if (tvTotalVaccinations != null) {
            tvTotalVaccinations.setText("0");
        }

        if (tvUpcomingVaccinations != null) {
            tvUpcomingVaccinations.setText("0");
        }

        if (tvAlertEmpty != null) {
            tvAlertEmpty.setVisibility(View.GONE);
        }

        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setVisibility(View.GONE);
        }
    }

    private void cancelCall(Call<?> call) {

        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    @Override
    public void onDestroyView() {

        cancelCall(petsCall);
        cancelCall(historyCall);
        cancelCall(alertsCall);
        cancelCall(createCall);

        pendingStampRecordId = null;

        super.onDestroyView();
    }
}