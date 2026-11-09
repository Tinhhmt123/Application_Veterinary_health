
package com.example.healthy_pet.ui.vaccine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/**
 * [Thành viên 3] Nghiệp vụ 3: Sổ tiêm chủng & Cảnh báo y tế tự động 7 ngày (VaccinationRecords)
 */

public class VaccineAlertFragment extends Fragment {

    private Spinner spinnerPets;
    private TextView tvError;
    private ProgressBar progressBar;
    private MaterialButton btnAddVaccination;

    private SessionManager sessionManager;
    private PetWeightApi petWeightApi;

    private Long userId;
    private Long selectedPetId;

    private final List<Pet> petList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_vaccine_alert,
                container,
                false
        );

        // Ánh xạ giao diện
        spinnerPets = view.findViewById(R.id.spinnerPets);
        tvError = view.findViewById(R.id.tvError);
        progressBar = view.findViewById(R.id.progressBar);
        btnAddVaccination = view.findViewById(R.id.btnAddVaccination);

        // Khởi tạo
        sessionManager = new SessionManager(requireContext());
        petWeightApi = RetrofitClient.getPetWeightApi();

        userId = sessionManager.getUserId();

        // Nút thêm mũi tiêm
        btnAddVaccination.setOnClickListener(v -> {
            if (selectedPetId == null) {
                Toast.makeText(
                        requireContext(),
                        "Vui lòng chọn thú cưng trước!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Toast.makeText(
                    requireContext(),
                    "Sẽ triển khai chức năng thêm mũi tiêm ở bước tiếp theo.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Kiểm tra người dùng
        if (userId == null || userId <= 0) {
            showError("Không tìm thấy ID người dùng. Vui lòng kiểm tra đăng nhập.");
        } else {
            loadPets();
        }

        return view;
    }

    // Lấy danh sách thú cưng từ Backend
    private void loadPets() {

        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        petWeightApi.getPetsByUser(userId).enqueue(
                new Callback<List<Pet>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<Pet>> call,
                            @NonNull Response<List<Pet>> response
                    ) {
                        if (!isAdded()) return;

                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {

                            petList.clear();
                            petList.addAll(response.body());

                            if (petList.isEmpty()) {
                                showError("Bạn chưa có thú cưng nào. Hãy thêm thú cưng trước.");
                                return;
                            }

                            setupPetSpinner();

                        } else {
                            showError(
                                    "Không thể tải danh sách thú cưng. Mã lỗi: "
                                            + response.code()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<Pet>> call,
                            @NonNull Throwable t
                    ) {
                        if (!isAdded()) return;

                        progressBar.setVisibility(View.GONE);
                        showError("Lỗi kết nối: " + t.getMessage());
                    }
                }
        );
    }

    // Hiển thị danh sách thú cưng trong Spinner
    private void setupPetSpinner() {

        List<String> petNames = new ArrayList<>();

        for (Pet pet : petList) {
            String name = pet.getName();

            if (name == null || name.trim().isEmpty()) {
                name = "Thú cưng";
            }

            String species = pet.getSpecies();

            if (species != null && !species.trim().isEmpty()) {
                name += " (" + species + ")";
            }

            petNames.add(name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                petNames
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPets.setAdapter(adapter);

        spinnerPets.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (position >= 0 && position < petList.size()) {

                            Pet selectedPet = petList.get(position);

                            selectedPetId = selectedPet.getId();

                            Toast.makeText(
                                    requireContext(),
                                    "Đã chọn: " + selectedPet.getName(),
                                    Toast.LENGTH_SHORT
                            ).show();

                            // Giai đoạn tiếp theo:
                            // Tải lịch sử tiêm và cảnh báo theo selectedPetId.
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                        selectedPetId = null;
                    }
                }
        );
    }

    // Hiển thị thông báo lỗi
    private void showError(String message) {
        if (!isAdded()) return;

        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
