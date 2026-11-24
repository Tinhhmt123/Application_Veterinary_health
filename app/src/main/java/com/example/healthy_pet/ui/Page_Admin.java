package com.example.healthy_pet.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.healthy_pet.MainActivity;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.ui.admin.AdminDashboardFragment;
import com.example.healthy_pet.ui.admin.Manage_Pet_Admin;
import com.example.healthy_pet.ui.auth.AccountFragment;
import com.example.healthy_pet.ui.component.CurvedBottomNavView;
import com.example.healthy_pet.ui.pet.Pet_Detail_Admin_User;
import java.util.Arrays;
import java.util.List;

/**
 * Trang chính dành cho ADMIN (gắn với page_admin.xml)
 * Gồm 3 phần: Header (trên) + Content (adminContentContainer ở giữa) + Footer cong (5 mục menu bên dưới)
 */
public class Page_Admin extends Fragment {

    private static final String KEY_SELECTED_TAB = "selected_admin_tab";
    private static final String KEY_TAB_SUBTITLE = "current_admin_subtitle";

    private TextView tvAdminHeaderSubtitle;
    private View rootView;
    private View btnAdminOpenDrawer;
    private View btnAdminHeaderBack;
    private CurvedBottomNavView footerAdmin;
    private int selectedTabIndex = 0;
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

        if (savedInstanceState != null) {
            selectedTabIndex = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            currentTabSubtitle = savedInstanceState.getString(KEY_TAB_SUBTITLE, currentTabSubtitle);
        }

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

        // 2. Footer cong + Vòng tròn nổi (5 mục menu nét viền mảnh của Admin)
        List<CurvedBottomNavView.NavItem> adminNavItems = Arrays.asList(
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_users_outline, "Khách hàng"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_paw_outline, "Thú cưng"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_stethoscope_outline, "Lịch & Đơn"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_clinic_outline, "Chi nhánh"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_user_outline, "Tài khoản")
        );
        footerAdmin.setupTabs(adminNavItems, true, selectedTabIndex);
        footerAdmin.setOnTabSelectedListener(this::selectAdminTab);

        if (savedInstanceState == null) {
            selectAdminTab(0);
        } else {
            syncHeaderAndFooterForBackStack();
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, selectedTabIndex);
        outState.putString(KEY_TAB_SUBTITLE, currentTabSubtitle);
    }

    private void selectAdminTab(int index) {
        selectedTabIndex = index;
        switch (index) {
            case 0:
                setCenterContent(AdminDashboardFragment.newInstance(6), "Quản lý Khách hàng");
                break;
            case 1:
                setCenterContent(new Manage_Pet_Admin(), "Quản lý Hồ sơ Thú cưng");
                break;
            case 2:
                setCenterContent(AdminDashboardFragment.newInstance(7), "Duyệt lịch hẹn & Kê đơn bệnh án");
                break;
            case 3:
                setCenterContent(AdminDashboardFragment.newInstance(8), "Quản lý Hệ thống Chi nhánh Phòng khám");
                break;
            case 4:
                setCenterContent(new AccountFragment(), "Tài khoản Quản trị viên");
                break;
            default:
                break;
        }
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
        selectedTabIndex = 2;
        if (footerAdmin != null) {
            footerAdmin.setSelectedIndex(2, true);
        }
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
}
