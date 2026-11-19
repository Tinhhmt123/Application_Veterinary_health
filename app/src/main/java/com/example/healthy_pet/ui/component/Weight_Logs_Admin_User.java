package com.example.healthy_pet.ui.component;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.Weight_Log_Adapter_Admin_User;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.models.WeightLog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Hộp thoại dùng chung cho cả Admin và User để xem, thêm, sửa, xóa Nhật ký cân nặng (gắn với weight_logs_admin_user.xml).
 */
public class Weight_Logs_Admin_User {

    public static void showWeightLogsDialog(@NonNull Fragment hostFragment,
                                            @NonNull Pet pet,
                                            @NonNull Runnable onWeightChangedCallback) {
        if (!hostFragment.isAdded() || pet.getId() == null) return;
        Context context = hostFragment.requireContext();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.weight_logs_admin_user, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvWeightDialogTitle);
        TextView tvCurrent = dialogView.findViewById(R.id.tvWeightDialogCurrent);
        TextInputEditText etWeight = dialogView.findViewById(R.id.etWeightDialogValue);
        TextInputEditText etDate = dialogView.findViewById(R.id.etWeightDialogDate);
        TextInputEditText etNote = dialogView.findViewById(R.id.etWeightDialogNote);
        MaterialButton btnCancelEdit = dialogView.findViewById(R.id.btnWeightDialogCancelEdit);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnWeightDialogSave);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBarWeightDialog);
        TextView tvEmpty = dialogView.findViewById(R.id.tvWeightDialogEmpty);
        RecyclerView rvLogs = dialogView.findViewById(R.id.rvWeightDialogLogs);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnWeightDialogClose);

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        tvTitle.setText("⚖️ Nhật ký cân nặng - " + pet.getName());
        double currentWeight = pet.getCurrentWeight() != null ? pet.getCurrentWeight() : 0.0;
        tvCurrent.setText(String.format(Locale.US, "Cân nặng hiện tại: %.1f kg", currentWeight));
        etDate.setText(todayStr);

        final WeightLog[] editingLogHolder = {null};

        Runnable resetForm = () -> {
            editingLogHolder[0] = null;
            etWeight.setText("");
            etDate.setText(todayStr);
            etNote.setText("");
            btnCancelEdit.setVisibility(View.GONE);
            btnSave.setText("Ghi nhận cân nặng");
        };

        final Weight_Log_Adapter_Admin_User[] adapterHolder = new Weight_Log_Adapter_Admin_User[1];
        Runnable reloadLogs = () -> loadLogsAndRefreshHeader(
                hostFragment, context, pet, progressBar, tvEmpty, tvCurrent, adapterHolder[0], onWeightChangedCallback);

        adapterHolder[0] = new Weight_Log_Adapter_Admin_User(new Weight_Log_Adapter_Admin_User.OnWeightLogActionListener() {
            @Override
            public void onEditLog(WeightLog log) {
                editingLogHolder[0] = log;
                etWeight.setText(log.getWeight() != null ? String.format(Locale.US, "%.1f", log.getWeight()) : "");
                etDate.setText(log.getRecordDate() != null ? log.getRecordDate() : todayStr);
                etNote.setText(log.getNote() != null ? log.getNote() : "");
                btnCancelEdit.setVisibility(View.VISIBLE);
                btnSave.setText("Cập nhật mốc");
            }

            @Override
            public void onDeleteLog(WeightLog log) {
                if (log.getId() == null) return;
                progressBar.setVisibility(View.VISIBLE);
                RetrofitClient.getPetWeightApi().deleteWeightLog(log.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (!hostFragment.isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            if (editingLogHolder[0] != null && log.getId().equals(editingLogHolder[0].getId())) {
                                resetForm.run();
                            }
                            Toast.makeText(context, "Đã xóa mốc cân nặng", Toast.LENGTH_SHORT).show();
                            reloadLogs.run();
                        } else {
                            Toast.makeText(context, "Không thể xóa (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!hostFragment.isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(context, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        rvLogs.setLayoutManager(new LinearLayoutManager(context));
        rvLogs.setAdapter(adapterHolder[0]);

        etDate.setOnClickListener(v -> Pet_Form_Admin_User.showDatePicker(context, etDate));
        btnCancelEdit.setOnClickListener(v -> resetForm.run());

        btnSave.setOnClickListener(v -> {
            String weightStr = etWeight.getText() != null ? etWeight.getText().toString().trim().replace(',', '.') : "";
            String dateStr = etDate.getText() != null ? etDate.getText().toString().trim() : todayStr;
            String noteStr = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            double weightVal;
            try {
                weightVal = Double.parseDouble(weightStr);
                if (weightVal <= 0) {
                    etWeight.setError("Cân nặng phải > 0");
                    etWeight.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etWeight.setError("Nhập số cân nặng hợp lệ");
                etWeight.requestFocus();
                return;
            }

            WeightLog payload = new WeightLog(null, pet.getId(), dateStr, weightVal, noteStr);
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);

            Call<WeightLog> call = (editingLogHolder[0] == null)
                    ? RetrofitClient.getPetWeightApi().addWeightLog(payload)
                    : RetrofitClient.getPetWeightApi().updateWeightLog(editingLogHolder[0].getId(), payload);

            call.enqueue(new Callback<WeightLog>() {
                @Override
                public void onResponse(@NonNull Call<WeightLog> call, @NonNull Response<WeightLog> response) {
                    if (!hostFragment.isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    if (response.isSuccessful()) {
                        boolean wasEditing = editingLogHolder[0] != null;
                        resetForm.run();
                        Toast.makeText(context,
                                wasEditing ? "Đã cập nhật mốc cân nặng!" : "Đã ghi nhận cân nặng mới!",
                                Toast.LENGTH_SHORT).show();
                        reloadLogs.run();
                    } else {
                        Toast.makeText(context, "Lưu thất bại (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<WeightLog> call, @NonNull Throwable t) {
                    if (!hostFragment.isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(context, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // Tải dữ liệu ban đầu (không kích hoạt callback reload danh sách cha lúc mới mở)
        loadLogsOnly(hostFragment, context, pet.getId(), progressBar, tvEmpty, adapterHolder[0]);
    }

    private static void loadLogsOnly(Fragment hostFragment,
                                     Context context,
                                     Long petId,
                                     ProgressBar progressBar,
                                     TextView tvEmpty,
                                     Weight_Log_Adapter_Admin_User adapter) {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getPetWeightApi().getWeightLogsByPet(petId).enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(@NonNull Call<List<WeightLog>> call, @NonNull Response<List<WeightLog>> response) {
                if (!hostFragment.isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setLogs(response.body());
                    tvEmpty.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WeightLog>> call, @NonNull Throwable t) {
                if (!hostFragment.isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(context, "Không tải được lịch sử cân nặng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void loadLogsAndRefreshHeader(Fragment hostFragment,
                                                 Context context,
                                                 Pet pet,
                                                 ProgressBar progressBar,
                                                 TextView tvEmpty,
                                                 TextView tvCurrent,
                                                 Weight_Log_Adapter_Admin_User adapter,
                                                 Runnable onWeightChangedCallback) {
        loadLogsOnly(hostFragment, context, pet.getId(), progressBar, tvEmpty, adapter);
        RetrofitClient.getPetWeightApi().getPetById(pet.getId()).enqueue(new Callback<Pet>() {
            @Override
            public void onResponse(@NonNull Call<Pet> call, @NonNull Response<Pet> response) {
                if (!hostFragment.isAdded() || !response.isSuccessful() || response.body() == null) return;
                double latest = response.body().getCurrentWeight() != null ? response.body().getCurrentWeight() : 0.0;
                pet.setCurrentWeight(latest);
                tvCurrent.setText(String.format(Locale.US, "Cân nặng hiện tại: %.1f kg", latest));
                onWeightChangedCallback.run();
            }

            @Override
            public void onFailure(@NonNull Call<Pet> call, @NonNull Throwable t) {
                onWeightChangedCallback.run();
            }
        });
    }
}
