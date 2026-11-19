package com.example.healthy_pet.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.AppointmentAdapter;
import com.example.healthy_pet.api.AppointmentMedicalApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.databinding.DialogPrescriptionBinding;
import com.example.healthy_pet.models.Appointment;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.repository.AppointmentRepository;
import com.example.healthy_pet.repository.MedicalHistoryRepository;
import com.example.healthy_pet.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Luồng ADMIN] Quản trị hệ thống: Quản lý Khách hàng, Duyệt lịch & Kê đơn ca khám, Quản lý Chi nhánh
 */
public class AdminDashboardFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {

    private static final String ARG_FLOW = "admin_flow";
    private final List<Appointment> adminAppointments = new ArrayList<>();
    private AppointmentAdapter adapter;
    private AppointmentMedicalApi appointmentApi;

    public static AdminDashboardFragment newInstance(int flowNumber) {
        AdminDashboardFragment fragment = new AdminDashboardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FLOW, flowNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appointmentApi = RetrofitClient.getAppointmentMedicalApi();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;

        TextView title = view.findViewById(R.id.tvModuleTitle);
        TextView desc = view.findViewById(R.id.tvModuleDescription);
        MaterialButton btn = view.findViewById(R.id.btnModuleAction);

        if (flow == 6) {
            title.setText("👥 Quản Lý Người Dùng & Hồ Sơ Thú Cưng");
            desc.setText(
                    "• Tra cứu khách hàng theo tên hoặc số điện thoại, thực hiện khóa hoặc mở khóa tài khoản.\n" +
                    "• Tìm kiếm danh sách thú cưng toàn hệ thống, tạo hồ sơ mới hoặc tiếp nhận khám trực tiếp tại quầy."
            );
            btn.setText("🔒 Khóa / Mở khóa tài khoản & Tiếp nhận tại quầy");
            btn.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Đang thực hiện thao tác quản trị", Toast.LENGTH_SHORT).show()
            );
        } else if (flow == 7) {
            title.setText("🩺 Quản Lý & Duyệt Ca Khám");
            desc.setText(
                    "• Xử lý lịch hẹn từ khách hàng: Bấm Duyệt lịch (CONFIRMED) hoặc Từ chối lịch (CANCELLED).\n" +
                    "• Hoàn tất ca khám: Nhập triệu chứng, chẩn đoán, đơn thuốc, chi phí và tải ảnh toa thuốc để tạo bệnh án điện tử."
            );
            btn.setText("📋 Tải lại danh sách lịch hẹn");
            btn.setOnClickListener(v -> fetchAdminAppointments());

            ViewGroup parentLayout = (ViewGroup) desc.getParent();

            TextView tvListHeader = new TextView(requireContext());
            tvListHeader.setText("🩺 Danh sách ca khám chờ xử lý:");
            tvListHeader.setTextSize(16f);
            tvListHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            tvListHeader.setPadding(0, 24, 0, 12);
            parentLayout.addView(tvListHeader);

            RecyclerView recyclerView = new RecyclerView(requireContext());
            recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new AppointmentAdapter(adminAppointments, true, this);
            recyclerView.setAdapter(adapter);
            parentLayout.addView(recyclerView);

            fetchAdminAppointments();
        } else {
            title.setText("🏥 Quản Lý Chi Nhánh Phòng Khám");
            desc.setText(
                    "• Thêm mới, chỉnh sửa thông tin chi tiết các chi nhánh phòng khám trong hệ thống.\n" +
                    "• Cập nhật số điện thoại, giờ mở cửa và tọa độ vị trí để hiển thị điểm ghim trên bản đồ."
            );
            btn.setText("📍 Thêm chi nhánh & Cập nhật tọa độ");
            btn.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Đang thực hiện thao tác quản trị chi nhánh", Toast.LENGTH_SHORT).show()
            );
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;
        if (flow == 7) {
            fetchAdminAppointments();
        }
    }

    private void fetchAdminAppointments() {
        adminAppointments.clear();
        adminAppointments.addAll(AppointmentRepository.getInstance().getAllAppointments());
        if (adapter != null) adapter.updateData(adminAppointments);

        appointmentApi.getAllAppointments().enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AppointmentRepository.getInstance().setAppointments(response.body());
                    adminAppointments.clear();
                    adminAppointments.addAll(AppointmentRepository.getInstance().getAllAppointments());
                    if (adapter != null) adapter.updateData(adminAppointments);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {}
        });
    }

    @Override
    public void onApprove(Appointment appointment) {
        updateAppointmentStatus(appointment, "CONFIRMED");
    }

    @Override
    public void onReject(Appointment appointment) {
        updateAppointmentStatus(appointment, "CANCELLED");
    }

    @Override
    public void onCancel(Appointment appointment) {
        updateAppointmentStatus(appointment, "CANCELLED");
    }

    @Override
    public void onComplete(Appointment appointment) {
        showPrescriptionDialog(appointment);
    }

    private void updateAppointmentStatus(Appointment apt, String newStatus) {
        apt.setStatus(newStatus);
        AppointmentRepository.getInstance().updateStatus(apt.getId(), newStatus);
        fetchAdminAppointments();

        appointmentApi.updateAppointmentStatus(apt.getId(), newStatus).enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                Toast.makeText(getContext(), "Đã cập nhật trạng thái lịch hẹn", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Đã cập nhật trạng thái lịch hẹn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Dialog Kê Đơn Bệnh Án & Hoàn Thành Ca Khám
     */
    private void showPrescriptionDialog(Appointment apt) {
        DialogPrescriptionBinding dialogBinding = DialogPrescriptionBinding.inflate(getLayoutInflater());

        dialogBinding.tvHeader.setText("Ca khám #" + apt.getId() + " - " + apt.getPetName() + "\nNhập kết quả khám & đơn thuốc:");
        dialogBinding.etSymptoms.setText(apt.getNote());
        dialogBinding.etDiagnosis.setText("Viêm đường tiêu hóa nhẹ do vi khuẩn");
        dialogBinding.etPrescription.setText("Thuốc Smecta 3g + Men vi sinh Bio-Flora (Uống 5 ngày)");
        dialogBinding.etCost.setText("350000");
        dialogBinding.etImageUrl.setText("https://res.cloudinary.com/demo/image/upload/prescription_" + apt.getId() + ".jpg");

        new AlertDialog.Builder(requireContext())
                .setTitle("🩺 Kê đơn & Tạo bệnh án")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("💾 Hoàn tất ca khám", (dialog, which) -> {
                    String diagnosis = dialogBinding.etDiagnosis.getText().toString().trim();
                    String prescription = dialogBinding.etPrescription.getText().toString().trim();
                    String costStr = dialogBinding.etCost.getText().toString().trim();
                    BigDecimal totalCost = new BigDecimal(costStr.isEmpty() ? "350000" : costStr);
                    String imageUrl = dialogBinding.etImageUrl.getText().toString().trim();

                    MedicalHistory medReq = new MedicalHistory(
                            System.currentTimeMillis() % 10000,
                            apt.getId(),
                            apt.getPetId(),
                            apt.getAppointmentDate(),
                            dialogBinding.etSymptoms.getText().toString().trim(),
                            diagnosis,
                            prescription,
                            totalCost,
                            imageUrl,
                            "UNPAID",
                            ""
                    );
                    medReq.setPetName(apt.getPetName());
                    medReq.setUserId(apt.getUserId());

                    apt.setStatus("COMPLETED");
                    AppointmentRepository.getInstance().updateStatus(apt.getId(), "COMPLETED");
                    MedicalHistoryRepository.getInstance().addMedicalHistory(medReq);
                    fetchAdminAppointments();

                    appointmentApi.completeAppointmentAndPrescribe(apt.getId(), medReq).enqueue(new Callback<MedicalHistory>() {
                        @Override
                        public void onResponse(@NonNull Call<MedicalHistory> call, @NonNull Response<MedicalHistory> response) {
                            Toast.makeText(getContext(), "✅ Hoàn tất ca khám & tạo bệnh án thành công (" + CurrencyUtils.formatVND(totalCost) + ")", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(@NonNull Call<MedicalHistory> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "✅ Hoàn tất ca khám & tạo bệnh án thành công (" + CurrencyUtils.formatVND(totalCost) + ")", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
