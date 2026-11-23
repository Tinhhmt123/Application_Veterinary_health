package com.example.healthy_pet.ui.vaccine;
import android.util.Log;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class VaccineAlertFragment extends Fragment {

    private Spinner spinnerPets;

    private TextView tvTotalVaccinations;
    private TextView tvUpcomingVaccinations;
    private TextView tvAlertEmpty;
    private TextView tvHistoryEmpty;
    private TextView tvError;

    private LinearLayout layoutAlerts;
    private LinearLayout layoutVaccinationHistory;

    private Button btnAddVaccination;
    private ProgressBar progressBar;

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

    private ActivityResultLauncher<String> imagePickerLauncher;

    private Uri selectedStampImageUri;

    private ImageView ivStampPreview;

    /** Hiển thị ảnh dấu mộc đã được lưu trong hồ sơ tiêm chủng. */
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedStampImageUri = uri;

                        if (ivStampPreview != null) {
                            ivStampPreview.setImageURI(uri);
                            ivStampPreview.setVisibility(View.VISIBLE);
                        }

                        showToast("Đã chọn ảnh dấu mộc.");
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

        initViews(view);

        sessionManager = new SessionManager(requireContext());

        petWeightApi = RetrofitClient.getPetWeightApi();
        vaccinationApi = RetrofitClient.getVaccinationApi();

        userId = sessionManager.getUserId();
        Log.d("VACCINE_DEBUG", "Current userId = " + userId);

        btnAddVaccination.setEnabled(false);

        btnAddVaccination.setOnClickListener(v -> {

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

        petsCall = petWeightApi.getPetsByUser(userId);

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
        btnAddVaccination.setEnabled(true);

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

                        loadVaccinationHistory();
                        loadHealthAlerts();
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

        historyCall = vaccinationApi.getVaccinationsByPet(
                requestedPetId
        );

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

            // Ghi lại URL ảnh Backend trả về để xác định dữ liệu có bị null/rỗng không.
            Log.d(
                    "VACCINE_IMAGE",
                    "Stamp URL từ API: " + record.getStampImageUrl()
            );

            // Hiển thị ảnh dấu mộc từ Backend
            addStampImage(
                    layoutVaccinationHistory,
                    record.getStampImageUrl()
            );
        }
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

        if (sessionManager != null
                && !sessionManager.isAlert7DaysEnabled()) {

            cancelCall(alertsCall);

            if (layoutAlerts != null) {
                layoutAlerts.removeAllViews();
            }

            if (tvUpcomingVaccinations != null) {
                tvUpcomingVaccinations.setText("0");
            }

            if (tvAlertEmpty != null) {

                tvAlertEmpty.setText(
                        "Bạn đã tắt cảnh báo tiêm chủng."
                );

                tvAlertEmpty.setVisibility(View.VISIBLE);
            }

            return;
        }

        if (userId == null || selectedPetId == null || !isAdded()) {
            return;
        }

        final Long requestedPetId = selectedPetId;

        cancelCall(alertsCall);

        alertsCall = vaccinationApi.getHealthAlerts(userId);

        alertsCall.enqueue(new Callback<List<VaccinationRecord>>() {

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

                    List<VaccinationRecord> petAlerts =
                            new ArrayList<>();

                    for (VaccinationRecord record : response.body()) {

                        if (requestedPetId.equals(record.getPetId())) {
                            petAlerts.add(record);
                        }
                    }

                    displayHealthAlerts(petAlerts);

                } else {

                    layoutAlerts.removeAllViews();

                    tvUpcomingVaccinations.setText("0");

                    tvAlertEmpty.setText(
                            "Không thể tải cảnh báo tiêm chủng. Mã lỗi: "
                                    + response.code()
                    );

                    tvAlertEmpty.setVisibility(View.VISIBLE);
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

                tvAlertEmpty.setText(
                        "Lỗi tải cảnh báo: " + t.getMessage()
                );

                tvAlertEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayHealthAlerts(
            List<VaccinationRecord> alerts
    ) {

        layoutAlerts.removeAllViews();

        tvUpcomingVaccinations.setText(
                String.valueOf(alerts.size())
        );

        if (alerts.isEmpty()) {

            tvAlertEmpty.setText(
                    "Không có mũi tiêm quá hạn hoặc sắp đến hạn."
            );

            tvAlertEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvAlertEmpty.setVisibility(View.GONE);

        for (VaccinationRecord record : alerts) {

            String nextDueDate = record.getNextDueDate();

            String information =
                    "⚠️ Vaccine: " + safeText(record.getVaccineName())
                            + "\n📅 Ngày tiêm tiếp theo: "
                            + safeText(nextDueDate)
                            + "\n🏥 Phòng khám: "
                            + safeText(record.getClinicName())
                            + "\n📌 Trạng thái: "
                            + getVaccinationStatus(nextDueDate);

            addInfoCard(
                    layoutAlerts,
                    information,
                    nextDueDate
            );
        }
    }

    private void showAddVaccinationDialog() {

        if (!isAdded() || selectedPetId == null) {
            return;
        }

        selectedStampImageUri = null;
        ivStampPreview = null;

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

        form.addView(edtVaccineName);
        form.addView(edtInjectionDate);
        form.addView(edtNextDueDate);
        form.addView(edtClinicName);

        Button btnChooseImage = new Button(requireContext());
        btnChooseImage.setText("Chọn ảnh dấu mộc");

        form.addView(btnChooseImage);

        ivStampPreview = new ImageView(requireContext());
        ivStampPreview.setVisibility(View.GONE);
        ivStampPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(160)
                );

        imageParams.setMargins(
                0,
                dpToPx(10),
                0,
                dpToPx(10)
        );

        form.addView(ivStampPreview, imageParams);

        AlertDialog dialog =
                new AlertDialog.Builder(requireContext())
                        .setTitle("Thêm hồ sơ tiêm chủng")
                        .setView(form)
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Lưu", null)
                        .create();

        btnChooseImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

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
                        vaccineName,
                        injectionDate,
                        nextDueDate,
                        clinicName,
                        selectedStampImageUri,
                        dialog
                );
            });
        });

        dialog.setOnDismissListener(d -> {
            ivStampPreview = null;
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
            String vaccineName,
            String injectionDate,
            String nextDueDate,
            String clinicName,
            Uri imageUri,
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

        RequestBody userIdBody =
                createPartFromString(
                        String.valueOf(userId)
                );

        RequestBody vaccineNameBody =
                createPartFromString(vaccineName);

        RequestBody injectionDateBody =
                createPartFromString(injectionDate);

        RequestBody nextDueDateBody =
                createPartFromString(nextDueDate);

        RequestBody clinicNameBody =
                createPartFromString(clinicName);

        MultipartBody.Part stampImagePart = null;

        if (imageUri != null) {

            try {

                stampImagePart = createStampImagePart(imageUri);

            } catch (Exception e) {

                isCreatingVaccination = false;

                btnAddVaccination.setEnabled(true);

                showError(
                        "Không thể đọc ảnh dấu mộc: " + e.getMessage()
                );

                return;
            }
        }

        showLoading(true);

        final Long requestedPetId = selectedPetId;

        createCall = vaccinationApi.createVaccination(
                petIdBody,
                vaccineNameBody,
                injectionDateBody,
                nextDueDateBody,
                clinicNameBody,
                stampImagePart
        );

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

                btnAddVaccination.setEnabled(selectedPetId != null);

                if (response.isSuccessful() && response.body() != null) {

                    dialog.dismiss();

                    showToast(
                            "Thêm hồ sơ tiêm chủng thành công!"
                    );

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

                btnAddVaccination.setEnabled(selectedPetId != null);

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

        if (layoutAlerts != null) {
            layoutAlerts.removeAllViews();
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

        ivStampPreview = null;

        super.onDestroyView();
    }
}