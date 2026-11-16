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

        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("🔔 Sổ Tiêm Chủng & Cảnh Báo Lịch Tiêm");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Theo dõi sổ tiêm phòng vaccine định kỳ của từng bé thú cưng.\n" +
                "• Tự động hiển thị thông báo nhắc nhở khi đến lịch tiêm nhắc lại trong 7 ngày tới."
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("💉 Ghi nhận mũi tiêm & Chụp tem vaccine");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đang kiểm tra lịch tiêm nhắc lại", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
