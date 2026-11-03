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

        ((TextView) view.findViewById(R.id.tvModuleBadge)).setText("NGHIỆP VỤ 1 • THÀNH VIÊN 1");
        ((TextView) view.findViewById(R.id.tvModuleTitle)).setText("👤 Quản lý Tài khoản Cá nhân");
        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Đăng ký tài khoản mới, đăng nhập qua API và lưu phiên vào SharedPreferences.\n" +
                "• Cập nhật họ tên, số điện thoại và tải ảnh đại diện mới lên Cloudinary qua Retrofit (@Multipart)."
        );
        ((TextView) view.findViewById(R.id.tvModuleTechInfo)).setText(
                "Thực thể: Users\n" +
                "API: AuthUserApi.java\n" +
                "• POST /api/auth/login\n" +
                "• POST /api/auth/register\n" +
                "• PUT  /api/users/{id} (@Multipart)"
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("📷 Cập nhật Hồ sơ & Avatar Cloudinary");
        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đã lưu phiên vào SharedPreferences!", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
