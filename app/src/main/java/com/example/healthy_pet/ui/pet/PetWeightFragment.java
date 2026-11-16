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

        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("🐶 Hồ Sơ Thú Cưng & Nhật Ký Cân Nặng");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Thêm mới, xem chi tiết và chỉnh sửa hồ sơ các bé thú cưng trong gia đình.\n" +
                "• Theo dõi chỉ số cân nặng định kỳ và tự động đồng bộ vào thông tin sức khỏe của thú cưng."
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("⚖️ Thêm thú cưng / Ghi nhận cân nặng");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đã đồng bộ thông tin cân nặng thú cưng", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
