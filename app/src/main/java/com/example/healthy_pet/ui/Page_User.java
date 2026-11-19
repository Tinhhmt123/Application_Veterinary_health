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
import com.example.healthy_pet.ui.auth.AccountFragment;
import com.example.healthy_pet.ui.map.ClinicBookingFragment;
import com.example.healthy_pet.ui.medical.MedicalHistoryFragment;
import com.example.healthy_pet.ui.pet.Manage_Pet_User;
import com.example.healthy_pet.ui.pet.Pet_Detail_Admin_User;
import com.example.healthy_pet.ui.vaccine.VaccineAlertFragment;

/**
 * Trang chính dành cho USER (gắn với page_user.xml)
 * Gồm 3 phần: Header (trên) + Content (userContentContainer ở giữa) + Footer (5 mục menu bên dưới)
 */
public class Page_User extends Fragment {

    private TextView tvUserHeaderSubtitle;
    private View rootView;
    private View btnUserOpenDrawer;
    private View btnUserHeaderBack;
    private View footerUser;
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

        // 2. Footer: Chọn các mục bên dưới để thay đổi Content ở giữa (userContentContainer)
        rootView.findViewById(R.id.navUserAlert).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserAlert);
            setCenterContent(new VaccineAlertFragment(), "Cảnh báo y tế & Sổ tiêm chủng");
        });

        rootView.findViewById(R.id.navUserPet).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserPet);
            setCenterContent(new Manage_Pet_User(), "Hồ sơ thú cưng & Cân nặng");
        });

        rootView.findViewById(R.id.navUserBookingCenter).setOnClickListener(v -> navigateToBooking());

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
        highlightUserTab(R.id.navUserBookingCenter);
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
