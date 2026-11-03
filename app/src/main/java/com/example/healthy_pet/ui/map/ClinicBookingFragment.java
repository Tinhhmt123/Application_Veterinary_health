package com.example.healthy_pet.ui.map;

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
 * [Thành viên 4 & 5] Nghiệp vụ 5: Bản đồ Phòng khám (Google Maps) & Đặt lịch dịch vụ (VetClinics, Appointments)
 */
public class ClinicBookingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);

        ((TextView) view.findViewById(R.id.tvModuleBadge)).setText("NGHIỆP VỤ 5 • THÀNH VIÊN 4 & 5");
        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("📅 Đặt lịch Khám/Spa & Bản đồ Phòng khám");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• [Thành viên 4] Tích hợp Google Maps hiển thị điểm ghim các chi nhánh VetClinics, xem giờ mở cửa và gọi điện trực tiếp (ACTION_DIAL).\n" +
                "• [Thành viên 5] Tạo yêu cầu đặt lịch hẹn Khám/Tiêm/Spa qua DatePickerDialog & TimePickerDialog (status = PENDING)."
        );
        ((TextView) view.findViewById(R.id.tvModuleTechInfo)).setText(
                "Thực thể: VetClinics, Appointments\n" +
                "API: ClinicMapApi.java & AppointmentMedicalApi.java\n" +
                "• GET  /api/clinics\n" +
                "• POST /api/appointments"
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("📅 Mở Hộp thoại Đặt lịch hẹn mới");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Tạo lịch hẹn mới (Trạng thái: PENDING)", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
