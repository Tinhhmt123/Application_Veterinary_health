package com.example.healthy_pet.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.R;
import com.google.android.material.button.MaterialButton;

/**
 * [Luồng ADMIN] Nghiệp vụ 6, 7, 8 dành cho Quản trị viên hệ thống
 */
public class AdminDashboardFragment extends Fragment {

    private static final String ARG_FLOW = "admin_flow";

    public static AdminDashboardFragment newInstance(int flowNumber) {
        AdminDashboardFragment fragment = new AdminDashboardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FLOW, flowNumber);
        fragment.setArguments(args);
        return fragment;
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
        } else if (flow == 7) {
            badge.setText("NGHIỆP VỤ 7 (ADMIN) • THÀNH VIÊN 5");
            title.setText("🩺 Vận hành Lịch hẹn & Kê đơn Bệnh án");
            desc.setText(
                    "• Duyệt lịch hẹn của User: Xác nhận (CONFIRMED), Từ chối (CANCELLED) hoặc Hoàn thành (COMPLETED).\n" +
                    "• Khi bấm Hoàn thành: Nhập triệu chứng, chẩn đoán, đơn thuốc, chi phí DECIMAL(18, 0) & ảnh toa thuốc đẩy thẳng sang MedicalHistories."
            );
            tech.setText("Thực thể: Appointments, MedicalHistories\nAPI: AppointmentMedicalApi\n• PUT  /api/admin/appointments/{id}/status\n• POST /api/admin/appointments/{id}/complete (@Multipart)");
            btn.setText("🩺 Hoàn thành ca khám & Đẩy sang Bệnh án");
        } else {
            badge.setText("NGHIỆP VỤ 8 (ADMIN) • THÀNH VIÊN 4");
            title.setText("🏥 Quản lý Hệ thống Chi nhánh Phòng khám");
            desc.setText(
                    "• Thêm mới, chỉnh sửa hoặc xóa các chi nhánh phòng khám (VetClinics).\n" +
                    "• Cập nhật tọa độ chính xác (latitude, longitude) và ảnh cơ sở để đồng bộ điểm ghim lên Google Maps."
            );
            tech.setText("Thực thể: VetClinics\nAPI: ClinicMapApi\n• POST/PUT/DELETE /api/admin/clinics (@Multipart)");
            btn.setText("📍 Thêm Chi nhánh & Đồng bộ Tọa độ Maps");
        }

        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đang thực thi thao tác Quản trị viên (NV " + flow + ")", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
