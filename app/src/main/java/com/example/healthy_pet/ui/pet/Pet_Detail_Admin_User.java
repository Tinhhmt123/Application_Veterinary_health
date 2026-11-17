package com.example.healthy_pet.ui.pet;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.R;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.models.WeightLog;
import com.example.healthy_pet.ui.Page_Admin;
import com.example.healthy_pet.ui.Page_User;
import com.example.healthy_pet.ui.component.Pet_Form_Admin_User;
import com.example.healthy_pet.ui.component.Weight_Logs_Admin_User;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Trang Chi tiết Thú cưng & Timeline Nhật ký Cân nặng hiển thị tràn màn hình dưới Header (gắn với pet_detail_admin_user.xml).
 */
public class Pet_Detail_Admin_User extends Fragment {

    private static final String ARG_PET = "arg_pet";
    private static final String ARG_IS_ADMIN = "arg_is_admin";

    private Pet currentPet;
    private boolean isAdminMode;
    private Pet_Form_Admin_User petFormAdminUser;

    private ImageView ivAvatar;
    private TextView tvNameSpecies;
    private TextView tvBreedGender;
    private TextView tvBirthDate;
    private TextView tvOwner;
    private TextView tvCurrentWeight;
    private ProgressBar progressBarLogs;
    private TextView tvLogsEmpty;
    private LinearLayout layoutTimeline;

    private final ActivityResultLauncher<String> pickPetAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (petFormAdminUser != null) {
                    petFormAdminUser.onImagePicked(uri);
                }
            });

    public static Pet_Detail_Admin_User newInstance(@NonNull Pet pet, boolean isAdminMode) {
        Pet_Detail_Admin_User fragment = new Pet_Detail_Admin_User();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PET, pet);
        args.putBoolean(ARG_IS_ADMIN, isAdminMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pet_detail_admin_user, container, false);

        if (getArguments() != null) {
            currentPet = (Pet) getArguments().getSerializable(ARG_PET);
            isAdminMode = getArguments().getBoolean(ARG_IS_ADMIN, false);
        }
        if (currentPet == null) {
            navigateBackToPetList();
            return view;
        }

        ivAvatar = view.findViewById(R.id.ivDetailPetAvatar);
        tvNameSpecies = view.findViewById(R.id.tvDetailPetNameSpecies);
        tvBreedGender = view.findViewById(R.id.tvDetailPetBreedGender);
        tvBirthDate = view.findViewById(R.id.tvDetailPetBirthDate);
        tvOwner = view.findViewById(R.id.tvDetailPetOwner);
        tvCurrentWeight = view.findViewById(R.id.tvDetailPetCurrentWeight);
        progressBarLogs = view.findViewById(R.id.progressBarDetailLogs);
        tvLogsEmpty = view.findViewById(R.id.tvDetailLogsEmpty);
        layoutTimeline = view.findViewById(R.id.layoutDetailWeightTimeline);

        petFormAdminUser = new Pet_Form_Admin_User(
                this,
                isAdminMode,
                currentPet.getUserId(),
                pickPetAvatarLauncher,
                this::loadDetailAndLogs
        );

        view.findViewById(R.id.btnDetailPetEdit).setOnClickListener(v ->
                petFormAdminUser.showPetFormDialog(currentPet, this::loadDetailAndLogs)
        );

        view.findViewById(R.id.btnDetailPetWeight).setOnClickListener(v ->
                Weight_Logs_Admin_User.showWeightLogsDialog(this, currentPet, this::loadDetailAndLogs)
        );

        view.findViewById(R.id.btnDetailPetDelete).setOnClickListener(v ->
                Pet_Form_Admin_User.confirmAndDeletePet(this, currentPet, this::navigateBackToPetList)
        );

        view.findViewById(R.id.btnDetailPetBooking).setOnClickListener(v -> navigateToBookingPage());

        bindPetHeader();
        loadDetailAndLogs();
        return view;
    }

    private void bindPetHeader() {
        if (!isAdded() || currentPet == null) return;
        Context context = requireContext();

        String species = currentPet.getSpecies() != null && !currentPet.getSpecies().isEmpty()
                ? currentPet.getSpecies() : "Thú cưng";
        tvNameSpecies.setText((currentPet.getName() != null ? currentPet.getName() : "Chưa đặt tên") + " (" + species + ")");

        String breed = currentPet.getBreed() != null && !currentPet.getBreed().trim().isEmpty()
                ? currentPet.getBreed().trim() : "Chưa rõ giống";
        String gender = currentPet.getGender() != null && !currentPet.getGender().trim().isEmpty()
                ? currentPet.getGender().trim() : "Chưa rõ";
        tvBreedGender.setText("Giống: " + breed + " • " + gender);

        String birth = currentPet.getBirthDate() != null && !currentPet.getBirthDate().trim().isEmpty()
                ? currentPet.getBirthDate().trim() : "Chưa cập nhật";
        tvBirthDate.setText("Ngày sinh: " + birth);

        double weight = currentPet.getCurrentWeight() != null ? currentPet.getCurrentWeight() : 0.0;
        tvCurrentWeight.setText(String.format(Locale.US, "%.1f kg", weight));

        if (isAdminMode) {
            String ownerName = currentPet.getOwnerName() != null && !currentPet.getOwnerName().trim().isEmpty()
                    ? currentPet.getOwnerName().trim() : "Khách #" + currentPet.getUserId();
            String ownerPhone = currentPet.getOwnerPhone() != null && !currentPet.getOwnerPhone().trim().isEmpty()
                    ? " • " + currentPet.getOwnerPhone().trim() : "";
            tvOwner.setText("Chủ nuôi: " + ownerName + ownerPhone);
            tvOwner.setVisibility(View.VISIBLE);
        } else {
            tvOwner.setVisibility(View.GONE);
        }

        if (currentPet.getAvatarUrl() != null && !currentPet.getAvatarUrl().isEmpty()) {
            ImageMultipartUtils.loadCircularAvatar(context, currentPet.getAvatarUrl(), ivAvatar);
        } else {
            ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    private void loadDetailAndLogs() {
        if (currentPet == null || currentPet.getId() == null) return;
        Long petId = currentPet.getId();
        progressBarLogs.setVisibility(View.VISIBLE);

        RetrofitClient.getPetWeightApi().getPetById(petId).enqueue(new Callback<Pet>() {
            @Override
            public void onResponse(@NonNull Call<Pet> call, @NonNull Response<Pet> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                currentPet = response.body();
                bindPetHeader();
            }

            @Override
            public void onFailure(@NonNull Call<Pet> call, @NonNull Throwable t) {}
        });

        RetrofitClient.getPetWeightApi().getWeightLogsByPet(petId).enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(@NonNull Call<List<WeightLog>> call, @NonNull Response<List<WeightLog>> response) {
                if (!isAdded()) return;
                progressBarLogs.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    renderTimelineRows(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WeightLog>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressBarLogs.setVisibility(View.GONE);
            }
        });
    }

    private void renderTimelineRows(List<WeightLog> logs) {
        if (!isAdded()) return;
        Context context = requireContext();
        layoutTimeline.removeAllViews();

        if (logs == null || logs.isEmpty()) {
            tvLogsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvLogsEmpty.setVisibility(View.GONE);

        float density = context.getResources().getDisplayMetrics().density;
        int pad = Math.round(10 * density);
        int marginBottom = Math.round(8 * density);

        for (int i = 0; i < logs.size(); i++) {
            WeightLog log = logs.get(i);
            boolean isLatest = (i == 0);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = (i == logs.size() - 1) ? 0 : marginBottom;
            row.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(12 * density);
            if (isLatest) {
                bg.setColor(ContextCompat.getColor(context, R.color.emerald_50));
                bg.setStroke(Math.round(1 * density), ContextCompat.getColor(context, R.color.emerald_500));
            } else {
                bg.setColor(ContextCompat.getColor(context, R.color.slate_100));
            }
            row.setBackground(bg);

            LinearLayout leftCol = new LinearLayout(context);
            leftCol.setOrientation(LinearLayout.VERTICAL);
            leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDate = new TextView(context);
            String dateStr = log.getRecordDate() != null ? log.getRecordDate() : "";
            tvDate.setText(isLatest ? dateStr + "  (Mới nhất)" : dateStr);
            tvDate.setTextSize(12f);
            tvDate.setTypeface(null, Typeface.BOLD);
            tvDate.setTextColor(ContextCompat.getColor(context,
                    isLatest ? R.color.emerald_700 : R.color.slate_800));
            leftCol.addView(tvDate);

            if (log.getNote() != null && !log.getNote().trim().isEmpty()) {
                TextView tvNote = new TextView(context);
                tvNote.setText(log.getNote().trim());
                tvNote.setTextSize(12f);
                tvNote.setTextColor(ContextCompat.getColor(context, R.color.slate_500));
                leftCol.addView(tvNote);
            }

            TextView tvWeight = new TextView(context);
            double w = log.getWeight() != null ? log.getWeight() : 0.0;
            tvWeight.setText(String.format(Locale.US, "%.1f kg", w));
            tvWeight.setTextSize(14f);
            tvWeight.setTypeface(null, Typeface.BOLD);
            tvWeight.setTextColor(ContextCompat.getColor(context, R.color.emerald_700));

            row.addView(leftCol);
            row.addView(tvWeight);
            layoutTimeline.addView(row);
        }
    }

    private void navigateBackToPetList() {
        if (!isAdded()) return;
        Fragment parent = getParentFragment();
        if (parent != null && parent.getChildFragmentManager().getBackStackEntryCount() > 0) {
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
