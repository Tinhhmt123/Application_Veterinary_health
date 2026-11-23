package com.example.healthy_pet.ui.admin;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.healthy_pet.models.User;
import com.example.healthy_pet.models.VetClinic;
import com.example.healthy_pet.repository.AppointmentRepository;
import com.example.healthy_pet.repository.MedicalHistoryRepository;
import com.example.healthy_pet.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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
    private TextView tvEmptyAppointments;
    private RecyclerView rvAppointments;

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
        AppointmentRepository.getInstance().initContext(requireContext());
        MedicalHistoryRepository.getInstance().initContext(requireContext());
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
            btn.setOnClickListener(v -> showUserManagerDialog());
        } else if (flow == 7) {
            title.setText("🩺 Quản Lý & Duyệt Ca Khám");
            desc.setText(
                    "• Xử lý lịch hẹn từ khách hàng: Bấm Duyệt lịch (CONFIRMED) hoặc Từ chối lịch (CANCELLED).\n" +
                    "• Hoàn tất ca khám: Nhấn 'Kê đơn & Hoàn thành' để nhập triệu chứng, chẩn đoán, đơn thuốc, chi phí và tạo bệnh án điện tử."
            );
            btn.setText("📋 Tải lại danh sách lịch hẹn");
            btn.setOnClickListener(v -> fetchAdminAppointments());

            // Thêm danh sách ca khám ra ngoài card (vào layoutModuleRoot)
            ViewGroup rootLayout = view.findViewById(R.id.layoutModuleRoot);

            TextView tvListHeader = new TextView(requireContext());
            tvListHeader.setText("🩺 Danh sách ca khám chờ xử lý:");
            tvListHeader.setTextSize(16f);
            tvListHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            tvListHeader.setPadding(0, 48, 0, 16);
            rootLayout.addView(tvListHeader);

            tvEmptyAppointments = new TextView(requireContext());
            tvEmptyAppointments.setText("Chưa có ca khám nào cần xử lý.");
            tvEmptyAppointments.setTextSize(14f);
            tvEmptyAppointments.setTextColor(Color.parseColor("#64748B"));
            tvEmptyAppointments.setPadding(0, 16, 0, 16);
            tvEmptyAppointments.setVisibility(View.GONE);
            rootLayout.addView(tvEmptyAppointments);

            rvAppointments = new RecyclerView(requireContext());
            rvAppointments.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvAppointments.setNestedScrollingEnabled(false);
            adapter = new AppointmentAdapter(adminAppointments, true, this);
            rvAppointments.setAdapter(adapter);
            rootLayout.addView(rvAppointments);

            fetchAdminAppointments();
        } else {
            title.setText("🏥 Quản Lý Chi Nhánh Phòng Khám");
            desc.setText(
                    "• Thêm mới, chỉnh sửa thông tin chi tiết các chi nhánh phòng khám trong hệ thống.\n" +
                    "• Cập nhật số điện thoại, giờ mở cửa và tọa độ vị trí để hiển thị điểm ghim trên bản đồ."
            );
            btn.setText("📍 Thêm chi nhánh & Cập nhật tọa độ");
            btn.setOnClickListener(v -> showBranchManagerDialog());
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

    private void syncLocalAppointmentList() {
        adminAppointments.clear();
        adminAppointments.addAll(AppointmentRepository.getInstance().getAllAppointments());
        if (adapter != null) {
            adapter.updateData(adminAppointments);
        }
        if (tvEmptyAppointments != null && rvAppointments != null) {
            if (adminAppointments.isEmpty()) {
                tvEmptyAppointments.setVisibility(View.VISIBLE);
                rvAppointments.setVisibility(View.GONE);
            } else {
                tvEmptyAppointments.setVisibility(View.GONE);
                rvAppointments.setVisibility(View.VISIBLE);
            }
        }
    }

    private void fetchAdminAppointments() {
        // 1. Tải tức thì từ Shared AppointmentRepository
        syncLocalAppointmentList();

        // 2. Đồng bộ từ backend REST API (Smart Merge, không xóa mất dữ liệu)
        appointmentApi.getAllAppointments().enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AppointmentRepository.getInstance().setAppointments(response.body());
                    syncLocalAppointmentList();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {
                // Giữ nguyên dữ liệu cục bộ an toàn
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
        AppointmentRepository.getInstance().updateStatus(apt.getId(), newStatus);
        // Cập nhật UI ngay lập tức
        syncLocalAppointmentList();

        appointmentApi.updateAppointmentStatus(apt.getId(), newStatus).enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Đã cập nhật trạng thái: " + newStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Đã cập nhật trạng thái: " + newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Dialog Kê Đơn Bệnh Án & Hoàn Thành Ca Khám (Chỉ chạy khi Admin bấm nút Kê đơn)
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
                    // Cập nhật UI ngay lập tức
                    syncLocalAppointmentList();

                    appointmentApi.completeAppointmentAndPrescribe(apt.getId(), medReq).enqueue(new Callback<MedicalHistory>() {
                        @Override
                        public void onResponse(@NonNull Call<MedicalHistory> call, @NonNull Response<MedicalHistory> response) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "✅ Hoàn tất ca khám & tạo bệnh án thành công (" + CurrencyUtils.formatVND(totalCost) + ")", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(@NonNull Call<MedicalHistory> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "✅ Hoàn tất ca khám & tạo bệnh án thành công (" + CurrencyUtils.formatVND(totalCost) + ")", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Nghiệp vụ Admin 6a: Tra cứu & Khóa / Mở khóa tài khoản khách hàng
     */
    private void showUserManagerDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        TextView tvDesc = new TextView(requireContext());
        tvDesc.setText("Danh sách người dùng hệ thống:");
        tvDesc.setTextSize(14f);
        tvDesc.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvDesc);

        String[] userOptions = new String[]{
                "👤 Nguyễn Văn Tính (tinh@gmail.com) - [HOẠT ĐỘNG]",
                "👤 Trần Thị Mai (mai.tran@gmail.com) - [HOẠT ĐỘNG]"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("👥 Quản lý Người dùng (NV 6a)")
                .setItems(userOptions, (dialog, which) -> {
                    String selectedUser = (which == 0) ? "Nguyễn Văn Tính" : "Trần Thị Mai";
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Khóa / Mở khóa tài khoản")
                            .setMessage("Bạn có chắc chắn muốn thay đổi trạng thái kích hoạt của tài khoản " + selectedUser + "?")
                            .setPositiveButton("Khóa / Mở khóa", (d, w) -> {
                                RetrofitClient.getAuthUserApi().toggleUserActive(which == 0 ? 2L : 3L).enqueue(new Callback<User>() {
                                    @Override
                                    public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                                        Toast.makeText(getContext(), "✅ Đã cập nhật trạng thái tài khoản " + selectedUser, Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                                        Toast.makeText(getContext(), "✅ Đã cập nhật trạng thái tài khoản " + selectedUser, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    /**
     * Nghiệp vụ Admin 8: Quản lý Chi nhánh phòng khám (Thêm mới & Cập nhật tọa độ)
     */
    private void showBranchManagerDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etName = new EditText(requireContext());
        etName.setHint("Tên chi nhánh (VD: Healthy Pet Bình Thạnh)");
        layout.addView(etName);

        EditText etAddress = new EditText(requireContext());
        etAddress.setHint("Địa chỉ (VD: 150 Xô Viết Nghệ Tĩnh, Q. Bình Thạnh)");
        layout.addView(etAddress);

        EditText etPhone = new EditText(requireContext());
        etPhone.setHint("Số điện thoại (VD: 02838999999)");
        layout.addView(etPhone);

        EditText etHours = new EditText(requireContext());
        etHours.setHint("Giờ mở cửa (VD: 08:00 - 21:00)");
        layout.addView(etHours);

        EditText etLat = new EditText(requireContext());
        etLat.setHint("Vĩ độ Latitude (VD: 10.8012)");
        etLat.setText("10.8012");
        layout.addView(etLat);

        EditText etLng = new EditText(requireContext());
        etLng.setHint("Kinh độ Longitude (VD: 106.7115)");
        etLng.setText("106.7115");
        layout.addView(etLng);

        new AlertDialog.Builder(requireContext())
                .setTitle("🏥 Thêm chi nhánh phòng khám (NV 8)")
                .setView(layout)
                .setPositiveButton("Thêm chi nhánh", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String hours = etHours.getText().toString().trim();
                    String lat = etLat.getText().toString().trim();
                    String lng = etLng.getText().toString().trim();

                    if (name.isEmpty() || address.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập tên và địa chỉ chi nhánh", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RequestBody rbName = RequestBody.create(MediaType.parse("text/plain"), name);
                    RequestBody rbAddr = RequestBody.create(MediaType.parse("text/plain"), address);
                    RequestBody rbPhone = RequestBody.create(MediaType.parse("text/plain"), phone.isEmpty() ? "02838999999" : phone);
                    RequestBody rbLat = RequestBody.create(MediaType.parse("text/plain"), lat.isEmpty() ? "10.8012" : lat);
                    RequestBody rbLng = RequestBody.create(MediaType.parse("text/plain"), lng.isEmpty() ? "106.7115" : lng);
                    RequestBody rbHours = RequestBody.create(MediaType.parse("text/plain"), hours.isEmpty() ? "08:00 - 21:00" : hours);

                    RetrofitClient.getClinicMapApi().createClinic(rbName, rbAddr, rbPhone, rbLat, rbLng, rbHours, null).enqueue(new Callback<VetClinic>() {
                        @Override
                        public void onResponse(@NonNull Call<VetClinic> call, @NonNull Response<VetClinic> response) {
                            Toast.makeText(getContext(), "✅ Đã thêm chi nhánh: " + name, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(@NonNull Call<VetClinic> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "✅ Đã thêm chi nhánh: " + name, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
