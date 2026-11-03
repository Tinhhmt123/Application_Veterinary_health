package com.example.healthy_pet.ui.pet;

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
 * [Thành viên 2] Nghiệp vụ 2: Quản lý Hồ sơ Thú cưng & Nhật ký Cân nặng (Pets, WeightLogs)
 */
public class PetWeightFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);

        ((TextView) view.findViewById(R.id.tvModuleBadge)).setText("NGHIỆP VỤ 2 • THÀNH VIÊN 2");
        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("🐶 Hồ sơ Thú cưng & Nhật ký Cân nặng");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Thêm mới, xem chi tiết, sửa hoặc xóa hồ sơ thú cưng; tải ảnh lên Cloudinary (@Multipart) và hiển thị qua Glide.\n" +
                "• Ghi nhận cân nặng định kỳ (WeightLogs) và tự động đồng bộ vào current_weight của thú cưng."
        );
        ((TextView) view.findViewById(R.id.tvModuleTechInfo)).setText(
                "Thực thể: Pets, WeightLogs\n" +
                "API: PetWeightApi.java\n" +
                "• GET/POST /api/pets (@Multipart)\n" +
                "• GET/POST /api/weight-logs (Sync current_weight)"
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("⚖️ Thêm Thú cưng / Ghi nhận Cân nặng");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đã đồng bộ WeightLogs -> Pets.current_weight!", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
