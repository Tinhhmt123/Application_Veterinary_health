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
