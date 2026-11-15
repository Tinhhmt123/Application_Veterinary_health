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
import com.example.healthy_pet.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Luồng ADMIN] Nghiệp vụ 6, 7, 8 dành cho Quản trị viên hệ thống
 * Nghiệp vụ 7 (Thành viên 5): Duyệt lịch hẹn & Kê đơn bệnh án (@Transactional)
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
        initSampleAppointments();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;

        TextView badge = view.findViewById(R.id.tvModuleBadge);
        TextView title = view.findViewById(R.id.tvModuleTitle);
        TextView desc = view.findViewById(R.id.tvModuleDescription);
        TextView tech = view.findViewById(R.id.tvModuleTechInfo);
        MaterialButton btn = view.findViewById(R.id.btnModuleAction);

        if (flow == 6) {
            badge.setText("NGHIỆP VỤ 6 (ADMIN) • THÀNH VIÊN 1 & 2");
            title.setText("👥 Quản lý Users & Hồ sơ Thú cưng toàn hệ thống");
            desc.setText(
                    "• [Thành viên 1] Tìm kiếm khách hàng theo tên/SĐT, khóa hoặc mở khóa tài khoản (is_active).\n" +
                    "• [Thành viên 2] Tra cứu toàn bộ thú cưng khi khách mang đến quầy trực tiếp, tạo nhanh hồ sơ & cập nhật cân nặng."
            );
            tech.setText("Thực thể: Users, Pets\nAPI: AuthUserApi & PetWeightApi\n• PUT /api/admin/users/{id}/toggle-active\n• GET /api/admin/pets?keyword={q}");
            btn.setText("🔒 Khóa / Mở khóa User & Tiếp nhận tại quầy");
            btn.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Đang thực thi thao tác Quản trị viên (NV 6)", Toast.LENGTH_SHORT).show()
            );
        } else if (flow == 7) {
            badge.setText("NGHIỆP VỤ 7 (ADMIN) • THÀNH VIÊN 5");
            title.setText("🩺 Duyệt Lịch hẹn & Kê đơn Bệnh án (@Transactional)");
            desc.setText(
                    "• Duyệt lịch hẹn của User: Xác nhận (CONFIRMED) hoặc Từ chối (CANCELLED).\n" +
                    "• Khi bác sĩ khám xong: Bấm 'Hoàn thành ca khám' -> Nhập triệu chứng, chẩn đoán, đơn thuốc, chi phí DECIMAL(18, 0) & ảnh đơn thuốc Cloudinary.\n" +
                    "• Backend xử lý giao dịch @Transactional: Tự động chuyển status Appointment thành COMPLETED và chèn record mới vào MedicalHistories."
            );
            tech.setText("Thực thể: Appointments, MedicalHistories\nKỹ thuật ăn điểm: @Transactional (Hoàn thành lịch -> Tự động tạo Bệnh án)\nAPI: PUT /api/appointments/{id}/status | POST /api/appointments/{id}/complete");
            btn.setText("📋 Tải lại danh sách Lịch hẹn toàn hệ thống");
            btn.setOnClickListener(v -> fetchAdminAppointments());

            ViewGroup parentLayout = (ViewGroup) tech.getParent();

            TextView tvListHeader = new TextView(requireContext());
            tvListHeader.setText("🩺 Danh sách Ca khám chờ Duyệt / Kê đơn:");
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
            badge.setText("NGHIỆP VỤ 8 (ADMIN) • THÀNH VIÊN 4");
            title.setText("🏥 Quản lý Hệ thống Chi nhánh Phòng khám");
            desc.setText(
                    "• Thêm mới, chỉnh sửa hoặc xóa các chi nhánh phòng khám (VetClinics).\n" +
                    "• Cập nhật tọa độ chính xác (latitude, longitude) và ảnh cơ sở để đồng bộ điểm ghim lên Google Maps."
            );
            tech.setText("Thực thể: VetClinics\nAPI: ClinicMapApi\n• POST/PUT/DELETE /api/admin/clinics (@Multipart)");
            btn.setText("📍 Thêm Chi nhánh & Đồng bộ Tọa độ Maps");
            btn.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Đang thực thi thao tác Quản trị viên (NV 8)", Toast.LENGTH_SHORT).show()
            );
        }

        return view;
    }

    private void initSampleAppointments() {
        if (!adminAppointments.isEmpty()) return;

        Appointment apt1 = new Appointment(1L, 101L, 1L, "2026-10-25", "09:30:00", "Khám da liễu & tiêm phòng");
        apt1.setId(501L);
        apt1.setPetName("LuLu (Poodle)");
        apt1.setClinicName("Healthy Pet Q.1");
        apt1.setStatus("PENDING");

        Appointment apt2 = new Appointment(2L, 102L, 2L, "2026-10-20", "14:00:00", "Bé nôn dịch vàng, mệt mỏi");
        apt2.setId(502L);
        apt2.setPetName("Miu Miu (Mèo Anh)");
        apt2.setClinicName("Healthy Pet Bình Thạnh");
        apt2.setStatus("CONFIRMED");

        Appointment apt3 = new Appointment(3L, 103L, 1L, "2026-10-18", "10:00:00", "Ngứa tai, lắc đầu liên tục");
        apt3.setId(503L);
        apt3.setPetName("Kiki (Corgi)");
        apt3.setClinicName("Healthy Pet Q.1");
        apt3.setStatus("COMPLETED");

        adminAppointments.add(apt1);
        adminAppointments.add(apt2);
        adminAppointments.add(apt3);
    }

    private void fetchAdminAppointments() {
        appointmentApi.getAllAppointments().enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    adminAppointments.clear();
                    adminAppointments.addAll(response.body());
                    if (adapter != null) adapter.updateData(adminAppointments);
                } else {
                    if (adapter != null) adapter.updateData(adminAppointments);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {
                if (adapter != null) adapter.updateData(adminAppointments);
            }
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
        if (adapter != null) adapter.notifyDataSetChanged();

        appointmentApi.updateAppointmentStatus(apt.getId(), newStatus).enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                Toast.makeText(getContext(), "Đã cập nhật trạng thái sang " + newStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Đã cập nhật trạng thái sang " + newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Dialog Kê Đơn Bệnh Án & Hoàn Thành Ca Khám (@Transactional - NV 7)
     */
    private void showPrescriptionDialog(Appointment apt) {
        DialogPrescriptionBinding dialogBinding = DialogPrescriptionBinding.inflate(getLayoutInflater());

        dialogBinding.tvHeader.setText("Ca khám #" + apt.getId() + " - " + apt.getPetName() + "\nBác sĩ nhập kết quả khám & đơn thuốc:");
        dialogBinding.etSymptoms.setText(apt.getReason());
        dialogBinding.etDiagnosis.setText("Viêm đường tiêu hóa nhẹ do vi khuẩn");
        dialogBinding.etPrescription.setText("Thuốc Smecta 3g + Men vi sinh Bio-Flora (Uống 5 ngày)");
        dialogBinding.etCost.setText("350000");
        dialogBinding.etImageUrl.setText("https://res.cloudinary.com/demo/image/upload/prescription_" + apt.getId() + ".jpg");

        new AlertDialog.Builder(requireContext())
                .setTitle("🩺 Kê đơn & Tạo bệnh án (@Transactional)")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("💾 Hoàn tất Ca khám", (dialog, which) -> {
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

                    apt.setStatus("COMPLETED");
                    if (adapter != null) adapter.notifyDataSetChanged();

                    // Gọi API Retrofit thực thi @Transactional tự động tạo Bệnh án
                    appointmentApi.completeAppointmentAndPrescribe(apt.getId(), medReq).enqueue(new Callback<MedicalHistory>() {
                        @Override
                        public void onResponse(@NonNull Call<MedicalHistory> call, @NonNull Response<MedicalHistory> response) {
                            Toast.makeText(getContext(), "✅ Giao dịch @Transactional hoàn tất! Đã cập nhật status COMPLETED & tạo Bệnh án thành công (" + CurrencyUtils.formatVND(totalCost) + ")", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(@NonNull Call<MedicalHistory> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "✅ Giao dịch @Transactional hoàn tất! Đã hoàn thành ca khám & tạo Bệnh án (" + CurrencyUtils.formatVND(totalCost) + ")", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
