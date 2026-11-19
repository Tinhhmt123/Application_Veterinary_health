package com.example.healthy_pet.ui.admin;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.google.android.material.button.MaterialButton;

/**
 * [Luồng ADMIN] Nghiệp vụ 6, 7, 8 dành cho Quản trị viên hệ thống
 */
public class AdminDashboardFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {

    private static final String ARG_FLOW = "admin_flow";
    private final List<Appointment> adminAppointments = new ArrayList<>();
    private AppointmentAdapter adapter;
    private AppointmentMedicalApi appointmentApi;

    private TextInputEditText etSearchUserKeyword;
    private TextView tvAdminUsersCount;
    private ProgressBar progressBarAdminUsers;
    private TextView tvEmptyAdminUsers;
    private MaterialButton btnFilterAllUsers;
    private MaterialButton btnFilterActiveUsers;
    private MaterialButton btnFilterLockedUsers;
    private UserAdminAdapter userAdminAdapter;
    private Boolean currentActiveFilter = null; // null = Tất cả, true = Đang hoạt động, false = Đã khóa

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
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;

        // Luồng 6: Giao diện Quản lý Users thật dành cho Admin (manage_user_admin.xml)
        if (flow == 6) {
            return setupManageUsersView(inflater, container);
        }

        // Luồng 7 & 8 (của Thành viên 4 & 5): Giữ nguyên khung hiện tại
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;

        TextView badge = view.findViewById(R.id.tvModuleBadge);
        TextView title = view.findViewById(R.id.tvModuleTitle);
        TextView desc = view.findViewById(R.id.tvModuleDescription);
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
        } else if (flow == 7) {
            badge.setText("NGHIỆP VỤ 7 (ADMIN) • THÀNH VIÊN 5");
            title.setText("🩺 Vận hành Lịch hẹn & Kê đơn Bệnh án");
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
}
