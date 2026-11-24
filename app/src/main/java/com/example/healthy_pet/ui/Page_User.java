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
import com.example.healthy_pet.ui.auth.AccountFragment;
import com.example.healthy_pet.ui.component.CurvedBottomNavView;
import com.example.healthy_pet.ui.map.ClinicBookingFragment;
import com.example.healthy_pet.ui.medical.MedicalHistoryFragment;
import com.example.healthy_pet.ui.pet.Manage_Pet_User;
import com.example.healthy_pet.ui.pet.Pet_Detail_Admin_User;
import com.example.healthy_pet.ui.vaccine.VaccineAlertFragment;
import java.util.Arrays;
import java.util.List;

/**
 * Trang chính dành cho USER (gắn với page_user.xml)
 * Gồm 3 phần: Header (trên) + Content (userContentContainer ở giữa) + Footer cong (5 mục menu bên dưới)
 */
public class Page_User extends Fragment {

    private static final String KEY_SELECTED_TAB = "selected_user_tab";
    private static final String KEY_TAB_SUBTITLE = "current_user_subtitle";

    private TextView tvUserHeaderSubtitle;
    private View rootView;
    private View btnUserOpenDrawer;
    private View btnUserHeaderBack;
    private CurvedBottomNavView footerUser;
    private int selectedTabIndex = 0;
    private String currentTabSubtitle = "Cảnh báo y tế & Sổ tiêm chủng";
    private OnBackPressedCallback detailBackCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.page_user, container, false);
        tvUserHeaderSubtitle = rootView.findViewById(R.id.tvUserHeaderSubtitle);
        btnUserOpenDrawer = rootView.findViewById(R.id.btnUserOpenDrawer);
        btnUserHeaderBack = rootView.findViewById(R.id.btnUserHeaderBack);
        footerUser = rootView.findViewById(R.id.footerUser);

        if (savedInstanceState != null) {
            selectedTabIndex = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            currentTabSubtitle = savedInstanceState.getString(KEY_TAB_SUBTITLE, currentTabSubtitle);
        }

        // 1. Header: Bấm icon 🐾 hoặc tiêu đề để mở Menu trượt bên trái; bấm ← khi đang xem trang con
        View.OnClickListener openDrawerListener = v -> {
            if (getChildFragmentManager().getBackStackEntryCount() > 0) return;
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDrawer();
            }
        };
        btnUserOpenDrawer.setOnClickListener(openDrawerListener);
        rootView.findViewById(R.id.layoutUserHeaderTitle).setOnClickListener(openDrawerListener);
        btnUserHeaderBack.setOnClickListener(v -> {
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

        // 2. Footer cong + Vòng tròn nổi (5 mục menu nét viền mảnh của User)
        List<CurvedBottomNavView.NavItem> userNavItems = Arrays.asList(
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_alert_outline, "Cảnh báo"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_paw_outline, "Thú cưng"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_calendar_outline, "Đặt lịch"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_medical_outline, "Bệnh án"),
                new CurvedBottomNavView.NavItem(R.drawable.ic_nav_user_outline, "Tài khoản")
        );
        footerUser.setupTabs(userNavItems, false, selectedTabIndex);
        footerUser.setOnTabSelectedListener(this::selectUserTab);

        if (savedInstanceState == null) {
            selectUserTab(0);
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

    private void selectUserTab(int index) {
        selectedTabIndex = index;
        switch (index) {
            case 0:
                setCenterContent(new VaccineAlertFragment(), "Cảnh báo y tế & Sổ tiêm chủng");
                break;
            case 1:
                setCenterContent(new Manage_Pet_User(), "Hồ sơ thú cưng & Cân nặng");
                break;
            case 2:
                setCenterContent(new ClinicBookingFragment(), "Đặt lịch Khám/Spa & Bản đồ");
                break;
            case 3:
                setCenterContent(new MedicalHistoryFragment(), "Lịch sử khám & Thanh toán");
                break;
            case 4:
                setCenterContent(new AccountFragment(), "Hồ sơ cá nhân");
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
        if (tvUserHeaderSubtitle != null) {
            tvUserHeaderSubtitle.setText(petTitle);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.userContentContainer, Pet_Detail_Admin_User.newInstance(pet, false))
                .addToBackStack("pet_detail")
                .commit();
    }

    public void navigateToBooking() {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStackImmediate();
        }
        selectedTabIndex = 2;
        if (footerUser != null) {
            footerUser.setSelectedIndex(2, true);
        }
        setCenterContent(new ClinicBookingFragment(), "Đặt lịch Khám/Spa & Bản đồ");
    }

    private void syncHeaderAndFooterForBackStack() {
        if (rootView == null) return;
        boolean inSubPage = getChildFragmentManager().getBackStackEntryCount() > 0;
        if (detailBackCallback != null) {
            detailBackCallback.setEnabled(inSubPage);
        }
        if (footerUser != null) {
            footerUser.setVisibility(inSubPage ? View.GONE : View.VISIBLE);
        }
        if (btnUserOpenDrawer != null) {
            btnUserOpenDrawer.setVisibility(inSubPage ? View.GONE : View.VISIBLE);
        }
        if (btnUserHeaderBack != null) {
            btnUserHeaderBack.setVisibility(inSubPage ? View.VISIBLE : View.GONE);
        }
        if (!inSubPage && tvUserHeaderSubtitle != null) {
            tvUserHeaderSubtitle.setText(currentTabSubtitle);
        }
    }

    /**
     * Thay đổi nội dung phần Content nằm ở giữa (userContentContainer)
     */
    private void setCenterContent(Fragment fragment, String subtitle) {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        currentTabSubtitle = subtitle;
        if (tvUserHeaderSubtitle != null) {
            tvUserHeaderSubtitle.setText(subtitle);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.userContentContainer, fragment)
                .commit();
    }
}
