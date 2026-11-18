package com.example.healthy_pet.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.healthy_pet.MainActivity;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.ui.admin.AdminDashboardFragment;
import com.example.healthy_pet.ui.admin.Manage_Pet_Admin;
import com.example.healthy_pet.ui.auth.AccountFragment;
import com.example.healthy_pet.ui.pet.Pet_Detail_Admin_User;

/**
 * Trang chính dành cho ADMIN (gắn với page_admin.xml)
 * Gồm 3 phần: Header (trên) + Content (adminContentContainer ở giữa) + Footer (5 mục menu bên dưới)
 */
public class Page_Admin extends Fragment {

    private TextView tvAdminHeaderSubtitle;
    private View rootView;
    private View btnAdminOpenDrawer;
    private View btnAdminHeaderBack;
    private View footerAdmin;
    private String currentTabSubtitle = "Quản lý Khách hàng";
    private OnBackPressedCallback detailBackCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.page_admin, container, false);
        tvAdminHeaderSubtitle = rootView.findViewById(R.id.tvAdminHeaderSubtitle);
        btnAdminOpenDrawer = rootView.findViewById(R.id.btnAdminOpenDrawer);
        btnAdminHeaderBack = rootView.findViewById(R.id.btnAdminHeaderBack);
        footerAdmin = rootView.findViewById(R.id.footerAdmin);

        // 1. Header: Bấm icon 🛡️ hoặc tiêu đề để mở Menu trượt bên trái; bấm ← khi đang xem trang con
        View.OnClickListener openDrawerListener = v -> {
            if (getChildFragmentManager().getBackStackEntryCount() > 0) return;
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDrawer();
            }
        };
        btnAdminOpenDrawer.setOnClickListener(openDrawerListener);
        rootView.findViewById(R.id.layoutAdminHeaderTitle).setOnClickListener(openDrawerListener);
        btnAdminHeaderBack.setOnClickListener(v -> {
            if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                getChildFragmentManager().popBackStack();
            }
        });

        detailBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                    getChildFragmentManager().popBackStack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), detailBackCallback);

        getChildFragmentManager().addOnBackStackChangedListener(this::syncHeaderAndFooterForBackStack);

        // 2. Footer: Chọn các mục bên dưới để thay đổi Content ở giữa (adminContentContainer)
        rootView.findViewById(R.id.navAdminUser).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminUser);
            setCenterContent(AdminDashboardFragment.newInstance(6), "Quản lý Khách hàng");
        });

        rootView.findViewById(R.id.navAdminPet).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminPet);
            setCenterContent(new Manage_Pet_Admin(), "Quản lý Hồ sơ Thú cưng");
        });

        rootView.findViewById(R.id.navAdminAppointment).setOnClickListener(v -> navigateToAppointment());

        rootView.findViewById(R.id.navAdminClinic).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminClinic);
            setCenterContent(AdminDashboardFragment.newInstance(8), "Quản lý Hệ thống Chi nhánh Phòng khám");
        });

        rootView.findViewById(R.id.navAdminAccount).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminAccount);
            setCenterContent(new AccountFragment(), "Tài khoản Quản trị viên");
        });

        // Mặc định hiển thị mục Quản lý Khách hàng khi vừa vào Page_Admin
        highlightAdminTab(R.id.navAdminUser);
        if (savedInstanceState == null) {
            setCenterContent(AdminDashboardFragment.newInstance(6), "Quản lý Khách hàng");
        } else {
            syncHeaderAndFooterForBackStack();
        }

        return rootView;
    }

    public void openPetDetailPage(@NonNull Pet pet) {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStackImmediate();
        }
        String petTitle = pet.getName() != null && !pet.getName().isEmpty()
                ? "Chi tiết thú cưng: " + pet.getName()
                : "Chi tiết thú cưng";
        if (tvAdminHeaderSubtitle != null) {
            tvAdminHeaderSubtitle.setText(petTitle);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.adminContentContainer, Pet_Detail_Admin_User.newInstance(pet, true))
                .addToBackStack("pet_detail")
                .commit();
    }

    public void navigateToAppointment() {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStackImmediate();
        }
        highlightAdminTab(R.id.navAdminAppointment);
        setCenterContent(AdminDashboardFragment.newInstance(7), "Duyệt lịch hẹn & Kê đơn bệnh án");
    }

    private void syncHeaderAndFooterForBackStack() {
        if (rootView == null) return;
        boolean inSubPage = getChildFragmentManager().getBackStackEntryCount() > 0;
        if (detailBackCallback != null) {
            detailBackCallback.setEnabled(inSubPage);
        }
        if (footerAdmin != null) {
            footerAdmin.setVisibility(inSubPage ? View.GONE : View.VISIBLE);
        }
        if (btnAdminOpenDrawer != null) {
            btnAdminOpenDrawer.setVisibility(inSubPage ? View.GONE : View.VISIBLE);
        }
        if (btnAdminHeaderBack != null) {
            btnAdminHeaderBack.setVisibility(inSubPage ? View.VISIBLE : View.GONE);
        }
        if (!inSubPage && tvAdminHeaderSubtitle != null) {
            tvAdminHeaderSubtitle.setText(currentTabSubtitle);
        }
    }

    /**
     * Thay đổi nội dung phần Content nằm ở giữa (adminContentContainer)
     */
    private void setCenterContent(Fragment fragment, String subtitle) {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        currentTabSubtitle = subtitle;
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
                R.id.navAdminUser,
                R.id.navAdminPet,
                R.id.navAdminAppointment,
                R.id.navAdminClinic,
                R.id.navAdminAccount
        };
        int[] labelIds = {
                R.id.tvNavAdminUser,
                R.id.tvNavAdminPet,
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
