package com.example.healthy_pet.ui.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.Pet_Adapter_Admin_User;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.ui.Page_Admin;
import com.example.healthy_pet.ui.component.Pet_Form_Admin_User;
import com.example.healthy_pet.ui.component.Weight_Logs_Admin_User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Luồng ADMIN] Nghiệp vụ 6b: Tra cứu & Quản lý Hồ sơ Thú cưng toàn hệ thống (gắn với manage_pet_admin.xml)
 */
public class Manage_Pet_Admin extends Fragment {

    private TextInputEditText etAdminPetKeyword;
    private TextView tvAdminPetsCount;
    private ProgressBar progressBarAdminPets;
    private TextView tvEmptyAdminPets;
    private SwipeRefreshLayout swipeRefreshAdminPets;

    private MaterialButton btnFilterPetAll;
    private MaterialButton btnFilterPetDog;
    private MaterialButton btnFilterPetCat;
    private MaterialButton btnFilterPetOther;

    private Pet_Adapter_Admin_User petAdapter;
    private Pet_Form_Admin_User petFormAdminUser;
    private String currentSpeciesFilter = null; // null = Tất cả, "Chó", "Mèo", "Khác"

    private final ActivityResultLauncher<String> pickPetAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (petFormAdminUser != null) {
                    petFormAdminUser.onImagePicked(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.manage_pet_admin, container, false);

        etAdminPetKeyword = view.findViewById(R.id.etAdminPetKeyword);
        tvAdminPetsCount = view.findViewById(R.id.tvAdminPetsCount);
        progressBarAdminPets = view.findViewById(R.id.progressBarAdminPets);
        tvEmptyAdminPets = view.findViewById(R.id.tvEmptyAdminPets);
        swipeRefreshAdminPets = view.findViewById(R.id.swipeRefreshAdminPets);
        RecyclerView rvAdminPets = view.findViewById(R.id.rvAdminPets);

        btnFilterPetAll = view.findViewById(R.id.btnFilterPetAll);
        btnFilterPetDog = view.findViewById(R.id.btnFilterPetDog);
        btnFilterPetCat = view.findViewById(R.id.btnFilterPetCat);
        btnFilterPetOther = view.findViewById(R.id.btnFilterPetOther);

        petFormAdminUser = new Pet_Form_Admin_User(
                this,
                true,
                null,
                pickPetAvatarLauncher,
                this::loadAdminPetsFromServer
        );

        petAdapter = new Pet_Adapter_Admin_User(true, new Pet_Adapter_Admin_User.OnPetActionListener() {
            @Override
            public void onPetClick(Pet pet) {
                if (getParentFragment() instanceof Page_Admin) {
                    ((Page_Admin) getParentFragment()).openPetDetailPage(pet);
                }
            }

            @Override
            public void onWeightClick(Pet pet) {
                Weight_Logs_Admin_User.showWeightLogsDialog(
                        Manage_Pet_Admin.this, pet, Manage_Pet_Admin.this::loadAdminPetsFromServer);
            }

            @Override
            public void onEditClick(Pet pet) {
                petFormAdminUser.showPetFormDialog(pet);
            }

            @Override
            public void onDeleteClick(Pet pet) {
                Pet_Form_Admin_User.confirmAndDeletePet(
                        Manage_Pet_Admin.this, pet, Manage_Pet_Admin.this::loadAdminPetsFromServer);
            }
        });

        rvAdminPets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdminPets.setAdapter(petAdapter);

        view.findViewById(R.id.btnAdminSearchPet).setOnClickListener(v -> loadAdminPetsFromServer());
        etAdminPetKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadAdminPetsFromServer();
                return true;
            }
            return false;
        });

        btnFilterPetAll.setOnClickListener(v -> selectSpeciesFilter(null, btnFilterPetAll));
        btnFilterPetDog.setOnClickListener(v -> selectSpeciesFilter("Chó", btnFilterPetDog));
        btnFilterPetCat.setOnClickListener(v -> selectSpeciesFilter("Mèo", btnFilterPetCat));
        btnFilterPetOther.setOnClickListener(v -> selectSpeciesFilter("Khác", btnFilterPetOther));

        view.findViewById(R.id.btnAdminAddPet).setOnClickListener(v ->
                petFormAdminUser.showPetFormDialog(null)
        );

        swipeRefreshAdminPets.setOnRefreshListener(this::loadAdminPetsFromServer);

        updateFilterButtonStyles(btnFilterPetAll);
        loadAdminPetsFromServer();

        return view;
    }

    private void selectSpeciesFilter(@Nullable String species, MaterialButton activeButton) {
        currentSpeciesFilter = species;
        updateFilterButtonStyles(activeButton);
        loadAdminPetsFromServer();
    }

    private void updateFilterButtonStyles(MaterialButton activeButton) {
        if (getContext() == null) return;
        MaterialButton[] buttons = {btnFilterPetAll, btnFilterPetDog, btnFilterPetCat, btnFilterPetOther};
        int activeColor = ContextCompat.getColor(requireContext(), R.color.indigo_600);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.slate_500);
        int activeBg = Color.parseColor("#EEF2FF");

        for (MaterialButton button : buttons) {
            boolean isActive = (button == activeButton);
            button.setTextColor(isActive ? activeColor : inactiveColor);
            button.setStrokeColor(ColorStateList.valueOf(isActive ? activeColor : inactiveColor));
            button.setBackgroundTintList(ColorStateList.valueOf(isActive ? activeBg : Color.TRANSPARENT));
        }
    }

    private void loadAdminPetsFromServer() {
        String keyword = etAdminPetKeyword != null && etAdminPetKeyword.getText() != null
                ? etAdminPetKeyword.getText().toString().trim()
                : "";
        String queryKeyword = keyword.isEmpty() ? null : keyword;

        if (!swipeRefreshAdminPets.isRefreshing()) {
            progressBarAdminPets.setVisibility(View.VISIBLE);
        }
        tvEmptyAdminPets.setVisibility(View.GONE);

        RetrofitClient.getPetWeightApi()
                .searchAllPets(queryKeyword, currentSpeciesFilter, null)
                .enqueue(new Callback<List<Pet>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Pet>> call, @NonNull Response<List<Pet>> response) {
                        if (!isAdded()) return;
                        progressBarAdminPets.setVisibility(View.GONE);
                        swipeRefreshAdminPets.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Pet> pets = response.body();
                            petAdapter.setPets(pets);
                            if (tvAdminPetsCount != null) {
                                tvAdminPetsCount.setText("🐾 Hồ sơ thú cưng hệ thống (" + pets.size() + " bé)");
                            }
                            tvEmptyAdminPets.setVisibility(pets.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            Toast.makeText(getContext(), "Không thể tải danh sách thú cưng (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Pet>> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBarAdminPets.setVisibility(View.GONE);
                        swipeRefreshAdminPets.setRefreshing(false);
                        Toast.makeText(getContext(), "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
