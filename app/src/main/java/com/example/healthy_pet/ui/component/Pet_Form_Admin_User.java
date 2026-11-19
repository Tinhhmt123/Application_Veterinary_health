package com.example.healthy_pet.ui.component;

import android.app.DatePickerDialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.healthy_pet.R;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.models.User;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Hộp thoại dùng chung cho cả Admin và User để Thêm / Sửa / Xóa Hồ sơ Thú cưng (gắn với pet_form_admin_user.xml).
 */
public class Pet_Form_Admin_User {

    private final Fragment hostFragment;
    private final boolean isAdminMode;
    private final Long defaultUserId;
    private final ActivityResultLauncher<String> pickImageLauncher;
    private final Runnable onSuccessCallback;

    private Uri selectedAvatarUri;
    private ImageView currentPreviewImageView;

    public Pet_Form_Admin_User(@NonNull Fragment hostFragment,
                               boolean isAdminMode,
                               @Nullable Long defaultUserId,
                               @NonNull ActivityResultLauncher<String> pickImageLauncher,
                               @NonNull Runnable onSuccessCallback) {
        this.hostFragment = hostFragment;
        this.isAdminMode = isAdminMode;
        this.defaultUserId = defaultUserId;
        this.pickImageLauncher = pickImageLauncher;
        this.onSuccessCallback = onSuccessCallback;
    }

    public void onImagePicked(@Nullable Uri uri) {
        if (uri == null || !hostFragment.isAdded()) return;
        selectedAvatarUri = uri;
        if (currentPreviewImageView != null) {
            Glide.with(hostFragment.requireContext())
                    .load(uri)
                    .transform(new CircleCrop())
                    .placeholder(R.mipmap.ic_launcher_round)
                    .into(currentPreviewImageView);
        }
    }

    public void showPetFormDialog(@Nullable Pet editingPet) {
        showPetFormDialog(editingPet, null);
    }

    public void showPetFormDialog(@Nullable Pet editingPet, @Nullable Runnable extraSuccessCallback) {
        if (!hostFragment.isAdded()) return;
        Context context = hostFragment.requireContext();
        selectedAvatarUri = null;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.pet_form_admin_user, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogPetTitle);
        LinearLayout layoutOwner = dialogView.findViewById(R.id.layoutDialogPetOwner);
        Spinner spOwner = dialogView.findViewById(R.id.spDialogPetOwner);
        currentPreviewImageView = dialogView.findViewById(R.id.ivDialogPetAvatar);
        MaterialButton btnChooseAvatar = dialogView.findViewById(R.id.btnDialogChoosePetAvatar);
        TextInputEditText etName = dialogView.findViewById(R.id.etDialogPetName);
        TextInputEditText etSpecies = dialogView.findViewById(R.id.etDialogPetSpecies);
        TextInputEditText etBreed = dialogView.findViewById(R.id.etDialogPetBreed);
        RadioButton rbMale = dialogView.findViewById(R.id.rbPetMale);
        RadioButton rbFemale = dialogView.findViewById(R.id.rbPetFemale);
        TextInputEditText etBirthDate = dialogView.findViewById(R.id.etDialogPetBirthDate);
        TextInputEditText etWeight = dialogView.findViewById(R.id.etDialogPetWeight);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBarDialogPet);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogPetCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnDialogPetSave);

        dialogView.findViewById(R.id.btnQuickSpeciesDog).setOnClickListener(v -> etSpecies.setText("Chó"));
        dialogView.findViewById(R.id.btnQuickSpeciesCat).setOnClickListener(v -> etSpecies.setText("Mèo"));

        final List<User> loadedOwners = new ArrayList<>();
        if (isAdminMode) {
            layoutOwner.setVisibility(View.VISIBLE);
            loadOwnersIntoSpinner(context, spOwner, loadedOwners,
                    editingPet != null ? editingPet.getUserId() : null);
        } else {
            layoutOwner.setVisibility(View.GONE);
        }

        if (editingPet != null) {
            tvTitle.setText("Cập nhật hồ sơ thú cưng");
            etName.setText(editingPet.getName());
            etSpecies.setText(editingPet.getSpecies());
            etBreed.setText(editingPet.getBreed());
            if ("Cái".equalsIgnoreCase(editingPet.getGender())) {
                rbFemale.setChecked(true);
            } else {
                rbMale.setChecked(true);
            }
            etBirthDate.setText(editingPet.getBirthDate());
            if (editingPet.getCurrentWeight() != null) {
                etWeight.setText(String.format(Locale.US, "%.1f", editingPet.getCurrentWeight()));
            }
            if (editingPet.getAvatarUrl() != null && !editingPet.getAvatarUrl().isEmpty()) {
                ImageMultipartUtils.loadCircularAvatar(context, editingPet.getAvatarUrl(), currentPreviewImageView);
            }
        } else {
            tvTitle.setText(isAdminMode ? "Thêm thú cưng cho khách hàng" : "Thêm thú cưng mới");
            etSpecies.setText("Chó");
            etWeight.setText("0.0");
        }

        etBirthDate.setOnClickListener(v -> showDatePicker(context, etBirthDate));
        btnChooseAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        currentPreviewImageView.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            Long targetUserId = defaultUserId;
            if (isAdminMode) {
                int pos = spOwner.getSelectedItemPosition();
                if (loadedOwners.isEmpty() || pos < 0 || pos >= loadedOwners.size()) {
                    Toast.makeText(context, "Vui lòng chờ tải danh sách khách hàng!", Toast.LENGTH_SHORT).show();
                    return;
                }
                targetUserId = loadedOwners.get(pos).getId();
            }

            if (targetUserId == null || targetUserId <= 0) {
                Toast.makeText(context, "Không xác định được tài khoản chủ nuôi!", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = textOf(etName);
            String species = textOf(etSpecies);
            String breed = textOf(etBreed);
            String gender = rbFemale.isChecked() ? "Cái" : "Đực";
            String birthDate = textOf(etBirthDate);
            String weightRaw = textOf(etWeight).replace(',', '.');

            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên thú cưng");
                etName.requestFocus();
                return;
            }
            if (species.isEmpty()) {
                etSpecies.setError("Vui lòng nhập loài");
                etSpecies.requestFocus();
                return;
            }

            double weightVal = 0.0;
            if (!weightRaw.isEmpty()) {
                try {
                    weightVal = Double.parseDouble(weightRaw);
                    if (weightVal < 0) {
                        etWeight.setError("Cân nặng không được âm");
                        etWeight.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    etWeight.setError("Cân nặng không hợp lệ");
                    etWeight.requestFocus();
                    return;
                }
            }

            MultipartBody.Part avatarPart = null;
            if (selectedAvatarUri != null) {
                try {
                    avatarPart = ImageMultipartUtils.uriToMultipart(context, selectedAvatarUri, "avatarImage");
                } catch (IOException e) {
                    Toast.makeText(context, "Không thể đọc ảnh đã chọn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            submitPetForm(context, dialog, progressBar, btnSave, editingPet,
                    targetUserId, name, species, breed, gender, birthDate, weightVal, avatarPart, extraSuccessCallback);
        });

        dialog.show();
    }

    private void submitPetForm(Context context,
                               AlertDialog dialog,
                               ProgressBar progressBar,
                               MaterialButton btnSave,
                               @Nullable Pet editingPet,
                               Long userId,
                               String name,
                               String species,
                               String breed,
                               String gender,
                               String birthDate,
                               double weight,
                               @Nullable MultipartBody.Part avatarPart,
                               @Nullable Runnable extraSuccessCallback) {
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        RequestBody rbUserId = ImageMultipartUtils.toTextRequestBody(String.valueOf(userId));
        RequestBody rbName = ImageMultipartUtils.toTextRequestBody(name);
        RequestBody rbSpecies = ImageMultipartUtils.toTextRequestBody(species);
        RequestBody rbBreed = ImageMultipartUtils.toTextRequestBody(breed);
        RequestBody rbGender = ImageMultipartUtils.toTextRequestBody(gender);
        RequestBody rbBirthDate = ImageMultipartUtils.toTextRequestBody(birthDate);
        RequestBody rbWeight = ImageMultipartUtils.toTextRequestBody(String.format(Locale.US, "%.2f", weight));

        Call<Pet> call = (editingPet == null)
                ? RetrofitClient.getPetWeightApi().createPet(rbUserId, rbName, rbSpecies, rbBreed, rbGender, rbBirthDate, rbWeight, avatarPart)
                : RetrofitClient.getPetWeightApi().updatePet(editingPet.getId(), rbUserId, rbName, rbSpecies, rbBreed, rbGender, rbBirthDate, rbWeight, avatarPart);

        call.enqueue(new Callback<Pet>() {
            @Override
            public void onResponse(@NonNull Call<Pet> call, @NonNull Response<Pet> response) {
                if (!hostFragment.isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(context,
                            editingPet == null ? "Đã thêm thú cưng mới!" : "Đã cập nhật hồ sơ thú cưng!",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    onSuccessCallback.run();
                    if (extraSuccessCallback != null) {
                        extraSuccessCallback.run();
                    }
                } else {
                    Toast.makeText(context, "Lưu hồ sơ thất bại (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pet> call, @NonNull Throwable t) {
                if (!hostFragment.isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(context, "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadOwnersIntoSpinner(Context context,
                                       Spinner spinner,
                                       List<User> loadedOwners,
                                       @Nullable Long preselectUserId) {
        RetrofitClient.getAuthUserApi().searchUsers(null, null).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (!hostFragment.isAdded() || !response.isSuccessful() || response.body() == null) return;
                loadedOwners.clear();
                List<String> labels = new ArrayList<>();
                int selectedIndex = 0;
                for (User user : response.body()) {
                    loadedOwners.add(user);
                    String phone = (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty())
                            ? user.getPhoneNumber()
                            : "Chưa có SĐT";
                    labels.add(user.getFullName() + " (" + phone + ") - #" + user.getId());
                    if (preselectUserId != null && preselectUserId.equals(user.getId())) {
                        selectedIndex = loadedOwners.size() - 1;
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        context, android.R.layout.simple_spinner_item, labels);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                if (!labels.isEmpty()) {
                    spinner.setSelection(selectedIndex);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                if (!hostFragment.isAdded()) return;
                Toast.makeText(context, "Không tải được danh sách khách hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void confirmAndDeletePet(@NonNull Fragment fragment,
                                           @NonNull Pet pet,
                                           @NonNull Runnable onDeletedCallback) {
        if (!fragment.isAdded() || pet.getId() == null) return;
        Context context = fragment.requireContext();

        new AlertDialog.Builder(context)
                .setTitle("Xóa hồ sơ thú cưng")
                .setMessage("Bạn chắc chắn muốn xóa hồ sơ của bé \"" + pet.getName() + "\"?\n(Không thể xóa nếu bé đã có lịch hẹn hoặc bệnh án)")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, which) ->
                        RetrofitClient.getPetWeightApi().deletePet(pet.getId()).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                                if (!fragment.isAdded()) return;
                                if (response.isSuccessful()) {
                                    Toast.makeText(context, "Đã xóa hồ sơ " + pet.getName(), Toast.LENGTH_SHORT).show();
                                    onDeletedCallback.run();
                                } else if (response.code() == 409) {
                                    Toast.makeText(context, "Không thể xóa thú cưng đã có lịch hẹn hoặc bệnh án!", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(context, "Xóa thất bại (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                                if (!fragment.isAdded()) return;
                                Toast.makeText(context, "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        })
                )
                .show();
    }

    public static void showDatePicker(Context context, TextInputEditText targetInput) {
        Calendar cal = Calendar.getInstance();
        String existing = targetInput.getText() != null ? targetInput.getText().toString().trim() : "";
        String[] parts = existing.split("-");
        if (parts.length == 3) {
            try {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1;
                int d = Integer.parseInt(parts[2]);
                cal.set(y, m, d);
            } catch (NumberFormatException ignored) {
            }
        }
        new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> targetInput.setText(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
