package com.example.healthy_pet.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.MainActivity;
import com.example.healthy_pet.R;
import com.example.healthy_pet.ui.admin.AdminDashboardFragment;
import com.example.healthy_pet.ui.auth.AccountFragment;

/**
 * Trang chính dành cho ADMIN (gắn với page_admin.xml)
 * Gồm 3 phần: Header (trên) + Content (adminContentContainer ở giữa) + Footer (4 mục menu bên dưới)
 */
public class Page_Admin extends Fragment {

    private TextView tvAdminHeaderSubtitle;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.page_admin, container, false);
        tvAdminHeaderSubtitle = rootView.findViewById(R.id.tvAdminHeaderSubtitle);

        // 1. Header: Bấm icon 🛡️ hoặc tiêu đề để mở Menu trượt bên trái
        View.OnClickListener openDrawerListener = v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDrawer();
            }
        };
        rootView.findViewById(R.id.btnAdminOpenDrawer).setOnClickListener(openDrawerListener);
        rootView.findViewById(R.id.layoutAdminHeaderTitle).setOnClickListener(openDrawerListener);

        // 2. Footer: Chọn các mục bên dưới để thay đổi Content ở giữa (adminContentContainer)
        rootView.findViewById(R.id.navAdminUserPet).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminUserPet);
            setCenterContent(AdminDashboardFragment.newInstance(6), "Quản lý Khách hàng & Hồ sơ Thú cưng");
        });

        rootView.findViewById(R.id.navAdminAppointment).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminAppointment);
            setCenterContent(AdminDashboardFragment.newInstance(7), "Duyệt lịch hẹn & Kê đơn bệnh án");
        });

        rootView.findViewById(R.id.navAdminClinic).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminClinic);
            setCenterContent(AdminDashboardFragment.newInstance(8), "Quản lý Hệ thống Chi nhánh Phòng khám");
        });

        rootView.findViewById(R.id.navAdminAccount).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminAccount);
            setCenterContent(new AccountFragment(), "Tài khoản Quản trị viên");
        });

        // Mặc định hiển thị mục Quản lý Users & Pets khi vừa vào Page_Admin
        highlightAdminTab(R.id.navAdminUserPet);
        if (savedInstanceState == null) {
            setCenterContent(AdminDashboardFragment.newInstance(6), "Quản lý Khách hàng & Hồ sơ Thú cưng");
        }

        return rootView;
    }

    /**
     * Thay đổi nội dung phần Content nằm ở giữa (adminContentContainer)
     */
    private void setCenterContent(Fragment fragment, String subtitle) {
        if (tvAdminHeaderSubtitle != null) {
            tvAdminHeaderSubtitle.setText(subtitle);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.adminContentContainer, fragment)
                .commit();
    }

    private void highlightAdminTab(int activeTabId) {
        if (rootView == null || getContext() == null) return;

        int[] tabIds = {
                R.id.navAdminUserPet,
                R.id.navAdminAppointment,
                R.id.navAdminClinic,
                R.id.navAdminAccount
        };
        int[] labelIds = {
                R.id.tvNavAdminUserPet,
                R.id.tvNavAdminAppointment,
                R.id.tvNavAdminClinic,
                R.id.tvNavAdminAccount
        };

        int activeColor = ContextCompat.getColor(requireContext(), R.color.emerald_500);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.slate_500);

        for (int i = 0; i < tabIds.length; i++) {
            View tabView = rootView.findViewById(tabIds[i]);
            TextView tvLabel = rootView.findViewById(labelIds[i]);
            boolean isSelected = (tabIds[i] == activeTabId);

            if (tvLabel != null) {
                tvLabel.setTextColor(isSelected ? activeColor : inactiveColor);
                tvLabel.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (tabView != null) {
                tabView.animate()
                        .scaleX(isSelected ? 1.08f : 1.0f)
                        .scaleY(isSelected ? 1.08f : 1.0f)
                        .alpha(isSelected ? 1.0f : 0.65f)
                        .setDuration(160)
                        .start();
            }
        }
    }
}
