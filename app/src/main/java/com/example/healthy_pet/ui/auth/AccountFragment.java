package com.example.healthy_pet.ui.auth;

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
 * [Thành viên 1] Nghiệp vụ 1: Quản lý tài khoản cá nhân (Users)
 */
public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);

        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("👤 Quản Lý Tài Khoản Cá Nhân");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Đăng nhập, quản lý thông tin tài khoản cá nhân và lưu phiên làm việc.\n" +
                "• Cập nhật thông tin họ tên, số điện thoại liên hệ và thay đổi ảnh đại diện cá nhân."
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("📷 Cập nhật thông tin & Ảnh đại diện");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đã cập nhật thông tin cá nhân", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
