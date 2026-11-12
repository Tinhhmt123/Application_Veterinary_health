
package com.example.healthy_pet.ui.vaccine;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VaccineAlertFragment extends Fragment {

    // =========================
    // VIEWS
    // =========================

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

    // =========================
    // API
    // =========================

    private PetWeightApi petWeightApi;
    private VaccinationApi vaccinationApi;

    // =========================
    // SESSION
    // =========================

    private SessionManager sessionManager;

    private Long userId;
    private Long selectedPetId;

    // =========================
    // DATA
    // =========================

    private final List<Pet> petList = new ArrayList<>();

    private Call<List<Pet>> petsCall;
    private Call<List<VaccinationRecord>> historyCall;
    private Call<List<VaccinationRecord>> alertsCall;
    private Call<VaccinationRecord> createCall;

    private boolean isLoadingPets = false;
    private boolean isCreatingVaccination = false;

    public VaccineAlertFragment() {
        // Required empty public constructor
    }

    // =====================================================
    // 1. CREATE VIEW
    // =====================================================

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

    // =====================================================
    // 2. INITIALIZE
    // =====================================================

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

        btnAddVaccination.setEnabled(false);

        btnAddVaccination.setOnClickListener(v -> {

            if (userId == null || selectedPetId == null) {
                showToast("Vui lòng chọn thú cưng trước!");
                return;
            }

            showAddVaccinationDialog();
        });

        // Kiểm tra người dùng đã đăng nhập chưa.
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

    // =====================================================
    // 3. LOAD PETS
    // =====================================================

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

    // =====================================================
    // 4. SETUP PET SPINNER
    // =====================================================

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

                        // Nếu vẫn chọn thú cưng hiện tại thì không tải lại.
                        if (newPetId.equals(selectedPetId)) {
                            return;
                        }

                        selectedPetId = newPetId;

                        hideError();

                        // Hủy request của thú cưng cũ.
                        cancelCall(historyCall);
                        cancelCall(alertsCall);

                        // Xóa dữ liệu cũ.
                        clearVaccinationData();

                        // Tải dữ liệu của thú cưng mới.
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

        // Chọn thú cưng đầu tiên.
        spinnerPets.setSelection(0);
    }

    // =====================================================
    // 5. LOAD VACCINATION HISTORY
    // =====================================================

    private void loadVaccinationHistory() {

        if (selectedPetId == null || userId == null || !isAdded()) {
            return;
        }

        final Long requestedPetId = selectedPetId;

        cancelCall(historyCall);

        historyCall = vaccinationApi.getVaccinationsByPet(
                requestedPetId,
                userId
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

                // Không hiển thị dữ liệu của thú cưng cũ.
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

    // =====================================================
    // 6. DISPLAY VACCINATION HISTORY
    // =====================================================

    private void displayVaccinationHistory(
            List<VaccinationRecord> records
    ) {

        layoutVaccinationHistory.removeAllViews();

        tvTotalVaccinations.setText(
                String.valueOf(records.size())
        );

        if (records.isEmpty()) {

            tvHistoryEmpty.setText(
                    "Chưa có lịch sử tiêm chủng."
            );

            tvHistoryEmpty.setVisibility(View.VISIBLE);

            return;
        }

        tvHistoryEmpty.setVisibility(View.GONE);

        for (VaccinationRecord record : records) {

            String information =
                    "💉 Vaccine: " + safeText(record.getVaccineName())
                            + "\n📅 Ngày tiêm: "
                            + safeText(record.getInjectionDate())
                            + "\n⏰ Ngày tiêm tiếp theo: "
                            + safeText(record.getNextDueDate())
                            + "\n🏥 Phòng khám: "
                            + safeText(record.getClinicName());

            addInfoCard(
                    layoutVaccinationHistory,
                    information
            );
        }
    }

    // =====================================================
    // 7. LOAD HEALTH ALERTS
    // =====================================================

    private void loadHealthAlerts() {

        if (userId == null || selectedPetId == null || !isAdded()) {
            return;
        }

        final Long requestedPetId = selectedPetId;

        cancelCall(alertsCall);

        // API trả về cảnh báo của tất cả thú cưng thuộc user.
        // Android sẽ lọc theo thú cưng đang được chọn.
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

    // =====================================================
    // 8. DISPLAY HEALTH ALERTS
    // =====================================================

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

            String information =
                    "⚠️ Vaccine: " + safeText(record.getVaccineName())
                            + "\n📅 Ngày tiêm tiếp theo: "
                            + safeText(record.getNextDueDate())
                            + "\n🏥 Phòng khám: "
                            + safeText(record.getClinicName());

            addInfoCard(
                    layoutAlerts,
                    information
            );
        }
    }

    // =====================================================
    // 9. ADD VACCINATION DIALOG
    // =====================================================

    private void showAddVaccinationDialog() {

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

        EditText edtNextDueDate =
                createEditText("Ngày tiêm tiếp theo (yyyy-MM-dd)");

        EditText edtClinicName =
                createEditText("Tên phòng khám");

        form.addView(edtVaccineName);
        form.addView(edtInjectionDate);
        form.addView(edtNextDueDate);
        form.addView(edtClinicName);

        AlertDialog dialog =
                new AlertDialog.Builder(requireContext())
                        .setTitle("Thêm hồ sơ tiêm chủng")
                        .setView(form)
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

    // =====================================================
    // 10. CREATE VACCINATION
    // =====================================================

    private void createVaccination(
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

        showLoading(true);

        final Long requestedPetId = selectedPetId;

        createCall = vaccinationApi.createVaccination(
                petIdBody,
                userIdBody,
                vaccineNameBody,
                injectionDateBody,
                nextDueDateBody,
                clinicNameBody,
                null
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

                    // Chỉ tải lại nếu vẫn đang chọn thú cưng đã thêm.
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

    // =====================================================
    // 11. HELPER METHODS
    // =====================================================

    private RequestBody createPartFromString(String value) {

        return RequestBody.create(
                MediaType.parse("text/plain"),
                value == null ? "" : value
        );
    }

    private void addInfoCard(
            LinearLayout parent,
            String information
    ) {

        TextView textView = new TextView(requireContext());

        textView.setText(information);

        textView.setTextSize(14);

        textView.setPadding(
                dpToPx(14),
                dpToPx(12),
                dpToPx(14),
                dpToPx(12)
        );

        textView.setBackgroundResource(
                android.R.drawable.dialog_holo_light_frame
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                dpToPx(10)
        );

        parent.addView(textView, params);
    }

    private String safeText(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Chưa cập nhật";
        }

        return value;
    }

    // Kiểm tra ngày yyyy-MM-dd, tương thích Android API 21.
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

    // =====================================================
    // 12. DESTROY VIEW
    // =====================================================

    @Override
    public void onDestroyView() {

        cancelCall(petsCall);
        cancelCall(historyCall);
        cancelCall(alertsCall);
        cancelCall(createCall);

        super.onDestroyView();
    }
}
