package com.example.healthy_pet.ui.pet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.Pet_Adapter_Admin_User;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.ui.Page_User;
import com.example.healthy_pet.ui.component.Pet_Form_Admin_User;
import com.example.healthy_pet.ui.component.Weight_Logs_Admin_User;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Luồng USER] Chuyên mục Quản lý Hồ sơ Thú cưng & Nhật ký Cân nặng (gắn với manage_pet_user.xml)
 */
public class Manage_Pet_User extends Fragment {

    private SessionManager sessionManager;
    private Pet_Adapter_Admin_User petAdapter;
    private Pet_Form_Admin_User petFormAdminUser;
    private TextView tvUserPetsCount;
    private ProgressBar progressBarUserPets;
    private TextView tvEmptyUserPets;
    private SwipeRefreshLayout swipeRefreshUserPets;
    private TextInputEditText etUserPetSearch;

    private final List<Pet> allUserPets = new ArrayList<>();

    private final ActivityResultLauncher<String> pickPetAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (petFormAdminUser != null) {
                    petFormAdminUser.onImagePicked(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.manage_pet_user, container, false);
        sessionManager = new SessionManager(requireContext());

        etUserPetSearch = view.findViewById(R.id.etUserPetSearch);
        tvUserPetsCount = view.findViewById(R.id.tvUserPetsCount);
        progressBarUserPets = view.findViewById(R.id.progressBarUserPets);
        tvEmptyUserPets = view.findViewById(R.id.tvEmptyUserPets);
        swipeRefreshUserPets = view.findViewById(R.id.swipeRefreshUserPets);
        RecyclerView rvUserPets = view.findViewById(R.id.rvUserPets);

        petFormAdminUser = new Pet_Form_Admin_User(
                this,
                false,
                sessionManager.getUserId(),
                pickPetAvatarLauncher,
                this::loadUserPetsFromServer
        );

        petAdapter = new Pet_Adapter_Admin_User(false, new Pet_Adapter_Admin_User.OnPetActionListener() {
            @Override
            public void onPetClick(Pet pet) {
                if (getParentFragment() instanceof Page_User) {
                    ((Page_User) getParentFragment()).openPetDetailPage(pet);
                }
            }

            @Override
            public void onWeightClick(Pet pet) {
                Weight_Logs_Admin_User.showWeightLogsDialog(
                        Manage_Pet_User.this, pet, Manage_Pet_User.this::loadUserPetsFromServer);
            }

            @Override
            public void onEditClick(Pet pet) {
                petFormAdminUser.showPetFormDialog(pet);
            }

            @Override
            public void onDeleteClick(Pet pet) {
                Pet_Form_Admin_User.confirmAndDeletePet(
                        Manage_Pet_User.this, pet, Manage_Pet_User.this::loadUserPetsFromServer);
            }
        });

        rvUserPets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUserPets.setAdapter(petAdapter);

        view.findViewById(R.id.btnUserAddPet).setOnClickListener(v ->
                petFormAdminUser.showPetFormDialog(null)
        );

        swipeRefreshUserPets.setOnRefreshListener(this::loadUserPetsFromServer);

        etUserPetSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyLocalFilter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadUserPetsFromServer();
        return view;
    }

    private void loadUserPetsFromServer() {
        Long userId = sessionManager.getUserId();
        if (userId <= 0) {
            swipeRefreshUserPets.setRefreshing(false);
            progressBarUserPets.setVisibility(View.GONE);
            tvEmptyUserPets.setVisibility(View.VISIBLE);
            return;
        }

        if (!swipeRefreshUserPets.isRefreshing()) {
            progressBarUserPets.setVisibility(View.VISIBLE);
        }

        RetrofitClient.getPetWeightApi().getPetsByUser(userId).enqueue(new Callback<List<Pet>>() {
            @Override
            public void onResponse(@NonNull Call<List<Pet>> call, @NonNull Response<List<Pet>> response) {
                if (!isAdded()) return;
                progressBarUserPets.setVisibility(View.GONE);
                swipeRefreshUserPets.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    allUserPets.clear();
                    allUserPets.addAll(response.body());
                    String query = etUserPetSearch.getText() != null ? etUserPetSearch.getText().toString() : "";
                    applyLocalFilter(query);
                } else {
                    Toast.makeText(getContext(), "Không thể tải danh sách thú cưng (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Pet>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressBarUserPets.setVisibility(View.GONE);
                swipeRefreshUserPets.setRefreshing(false);
                Toast.makeText(getContext(), "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyLocalFilter(String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        if (kw.isEmpty()) {
            petAdapter.setPets(allUserPets);
            if (tvUserPetsCount != null) {
                tvUserPetsCount.setText("🐾 Thú cưng của bạn (" + allUserPets.size() + " bé)");
            }
            tvEmptyUserPets.setVisibility(allUserPets.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        List<Pet> filtered = new ArrayList<>();
        for (Pet pet : allUserPets) {
            boolean matchName = pet.getName() != null && pet.getName().toLowerCase(Locale.ROOT).contains(kw);
            boolean matchSpecies = pet.getSpecies() != null && pet.getSpecies().toLowerCase(Locale.ROOT).contains(kw);
            boolean matchBreed = pet.getBreed() != null && pet.getBreed().toLowerCase(Locale.ROOT).contains(kw);
            if (matchName || matchSpecies || matchBreed) {
                filtered.add(pet);
            }
        }
        petAdapter.setPets(filtered);
        if (tvUserPetsCount != null) {
            tvUserPetsCount.setText("🐾 Thú cưng của bạn (" + filtered.size() + " bé)");
        }
        tvEmptyUserPets.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
