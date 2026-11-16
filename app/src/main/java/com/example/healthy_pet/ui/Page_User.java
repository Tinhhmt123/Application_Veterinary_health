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
import com.example.healthy_pet.ui.auth.AccountFragment;
import com.example.healthy_pet.ui.map.ClinicBookingFragment;
import com.example.healthy_pet.ui.medical.MedicalHistoryFragment;
import com.example.healthy_pet.ui.pet.PetWeightFragment;
import com.example.healthy_pet.ui.vaccine.VaccineAlertFragment;

/**
 * Trang chính dành cho USER (gắn với page_user.xml)
 * Gồm 3 phần: Header (trên) + Content (userContentContainer ở giữa) + Footer (5 mục menu bên dưới)
 */
public class Page_User extends Fragment {

    private TextView tvUserHeaderSubtitle;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.page_user, container, false);
        tvUserHeaderSubtitle = rootView.findViewById(R.id.tvUserHeaderSubtitle);

        // 1. Header: Bấm icon 🐾 hoặc tiêu đề để mở Menu trượt bên trái
        View.OnClickListener openDrawerListener = v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDrawer();
            }
        };
        rootView.findViewById(R.id.btnUserOpenDrawer).setOnClickListener(openDrawerListener);
        rootView.findViewById(R.id.layoutUserHeaderTitle).setOnClickListener(openDrawerListener);

        // 2. Footer: Chọn các mục bên dưới để thay đổi Content ở giữa (userContentContainer)
        rootView.findViewById(R.id.navUserAlert).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserAlert);
            setCenterContent(new VaccineAlertFragment(), "Cảnh báo y tế & Sổ tiêm chủng");
        });

        rootView.findViewById(R.id.navUserPet).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserPet);
            setCenterContent(new PetWeightFragment(), "Hồ sơ thú cưng & Cân nặng");
        });

        rootView.findViewById(R.id.navUserBookingCenter).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserBookingCenter);
            setCenterContent(new ClinicBookingFragment(), "Đặt lịch Khám/Spa & Bản đồ");
        });

        rootView.findViewById(R.id.navUserMedical).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserMedical);
            setCenterContent(new MedicalHistoryFragment(), "Lịch sử khám & Thanh toán");
        });

        rootView.findViewById(R.id.navUserAccount).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserAccount);
            setCenterContent(new AccountFragment(), "Hồ sơ cá nhân");
        });

        // Mặc định hiển thị mục đầu tiên khi vừa vào Page_User
        highlightUserTab(R.id.navUserAlert);
        if (savedInstanceState == null) {
            setCenterContent(new VaccineAlertFragment(), "Cảnh báo y tế & Sổ tiêm chủng");
        }

        return rootView;
    }

    /**
     * Thay đổi nội dung phần Content nằm ở giữa (userContentContainer)
     */
    private void setCenterContent(Fragment fragment, String subtitle) {
        if (tvUserHeaderSubtitle != null) {
            tvUserHeaderSubtitle.setText(subtitle);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.userContentContainer, fragment)
                .commit();
    }

    private void highlightUserTab(int activeTabId) {
        if (rootView == null || getContext() == null) return;

        int[] tabIds = {
                R.id.navUserAlert,
                R.id.navUserPet,
                R.id.navUserBookingCenter,
                R.id.navUserMedical,
                R.id.navUserAccount
        };
        int[] labelIds = {
                R.id.tvNavUserAlert,
                R.id.tvNavUserPet,
                R.id.tvNavUserBooking,
                R.id.tvNavUserMedical,
                R.id.tvNavUserAccount
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
