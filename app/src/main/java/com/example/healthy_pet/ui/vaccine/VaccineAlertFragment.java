package com.example.healthy_pet.ui.vaccine;

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
 * [Thành viên 3] Nghiệp vụ 3: Sổ tiêm chủng & Cảnh báo y tế tự động 7 ngày (VaccinationRecords)
 */
public class VaccineAlertFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);

        ((TextView) view.findViewById(R.id.tvModuleBadge)).setText("NGHIỆP VỤ 3 • THÀNH VIÊN 3");
        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("🔔 Cảnh báo Y tế 7 ngày & Sổ Tiêm chủng");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Tự động quét next_due_date và hiển thị thẻ cảnh báo ĐỎ (Quá hạn) / VÀNG (Sắp đến hạn ≤ 7 ngày) ngay màn hình chính.\n" +
                "• Ghi nhận mũi tiêm vaccine mới và chụp ảnh tem/sổ tiêm giấy lên Cloudinary."
        );
        ((TextView) view.findViewById(R.id.tvModuleTechInfo)).setText(
                "Thực thể: VaccinationRecords\n" +
                "API: VaccinationApi.java\n" +
                "• GET  /api/vaccinations/alerts?userId={id}\n" +
                "• POST /api/vaccinations (@Multipart stamp)"
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("💉 Ghi nhận Mũi tiêm & Chụp tem Vaccine");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đang quét cảnh báo tiêm chủng 7 ngày tới!", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
