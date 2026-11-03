package com.example.healthy_pet.ui.medical;

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
import com.example.healthy_pet.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import java.math.BigDecimal;

/**
 * [Thành viên 5] Nghiệp vụ 4: Quản lý Bệnh án & Thống kê Chi phí y tế DECIMAL(18, 0) (MedicalHistories)
 */
public class MedicalHistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);

        String sampleTotal = CurrencyUtils.formatVND(new BigDecimal("770000"));

        ((TextView) view.findViewById(R.id.tvModuleBadge)).setText("NGHIỆP VỤ 4 • THÀNH VIÊN 5");
        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("📋 Bệnh án & Thống kê Chi phí (" + sampleTotal + ")");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Tra cứu lịch sử các lần khám chữa bệnh của thú cưng (triệu chứng, chẩn đoán bác sĩ, đơn thuốc).\n" +
                "• Xem ảnh chụp toa thuốc trên Cloudinary (Glide) và tổng hợp chi phí chính xác bằng DECIMAL(18, 0)."
        );
        ((TextView) view.findViewById(R.id.tvModuleTechInfo)).setText(
                "Thực thể: MedicalHistories\n" +
                "API: AppointmentMedicalApi.java\n" +
                "• GET /api/medical-histories?petId={id}\n" +
                "• Kiểu tiền tệ: BigDecimal <-> DECIMAL(18, 0)"
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("🧾 Xem Ảnh chụp Toa thuốc (Cloudinary)");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Tổng chi phí y tế: " + sampleTotal, Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
