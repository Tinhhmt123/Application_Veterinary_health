package com.example.healthy_pet.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter hiển thị danh sách Hồ sơ Thú cưng cho cả User và Admin (gắn với item_pet_admin_user.xml).
 * - Mặc định ẩn cột nút bên phải.
 * - Nhấn giữ (đè) hoặc hover chuột ~1 giây -> hiện tab nhỏ 3 nút (Cân nặng, Sửa, Xóa) bên phải.
 * - Bấm (chạm nhẹ) vào thẻ thú cưng -> mở trang Chi tiết thú cưng & Nhật ký cân nặng.
 */
public class Pet_Adapter_Admin_User extends RecyclerView.Adapter<Pet_Adapter_Admin_User.PetViewHolder> {

    private static final long ACTION_SHOW_DELAY_MS = 1000L;

    public interface OnPetActionListener {
        void onPetClick(Pet pet);
        void onWeightClick(Pet pet);
        void onEditClick(Pet pet);
        void onDeleteClick(Pet pet);
    }

    private final List<Pet> petList = new ArrayList<>();
    private final boolean isAdminMode;
    private final OnPetActionListener listener;
    private final Handler actionHandler = new Handler(Looper.getMainLooper());

    private Long expandedPetId = null;
    private boolean expandedByTouch = false;
    private Long pendingHoverPetId = null;
    private Runnable pendingHoverRunnable = null;
    private Runnable pendingHoldRunnable = null;

    public Pet_Adapter_Admin_User(boolean isAdminMode, OnPetActionListener listener) {
        this.isAdminMode = isAdminMode;
        this.listener = listener;
    }

    public void setPets(List<Pet> pets) {
        cancelPendingHover();
        cancelPendingHold();
        petList.clear();
        if (pets != null) {
            petList.addAll(pets);
        }
        if (expandedPetId != null && findPositionByPetId(expandedPetId) == -1) {
            expandedPetId = null;
            expandedByTouch = false;
        }
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        cancelPendingHover();
        cancelPendingHold();
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet_admin_user, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);
        Context context = holder.itemView.getContext();
        Long petId = pet.getId();

        holder.tvName.setText(pet.getName() != null ? pet.getName() : "Chưa đặt tên");

        double weight = pet.getCurrentWeight() != null ? pet.getCurrentWeight() : 0.0;
        holder.tvWeightBadge.setText(String.format(Locale.US, "%.1f kg", weight));

        StringBuilder info = new StringBuilder();
        info.append(pet.getSpecies() != null && !pet.getSpecies().isEmpty() ? pet.getSpecies() : "Thú cưng");
        if (pet.getBreed() != null && !pet.getBreed().trim().isEmpty()) {
            info.append(" • ").append(pet.getBreed().trim());
        }
        if (pet.getGender() != null && !pet.getGender().trim().isEmpty()) {
            info.append(" • ").append(pet.getGender().trim());
        }
        holder.tvSpeciesBreed.setText(info.toString());

        String birthText = (pet.getBirthDate() != null && !pet.getBirthDate().trim().isEmpty())
                ? "Ngày sinh: " + pet.getBirthDate().trim()
                : "Ngày sinh: Chưa cập nhật";
        holder.tvBirthDate.setText(birthText);

        if (isAdminMode) {
            String ownerName = (pet.getOwnerName() != null && !pet.getOwnerName().trim().isEmpty())
                    ? pet.getOwnerName().trim()
                    : "Khách #" + pet.getUserId();
            String ownerPhone = (pet.getOwnerPhone() != null && !pet.getOwnerPhone().trim().isEmpty())
                    ? " • " + pet.getOwnerPhone().trim()
                    : "";
            holder.tvOwner.setText("Chủ: " + ownerName + ownerPhone);
            holder.tvOwner.setVisibility(View.VISIBLE);
        } else {
            holder.tvOwner.setVisibility(View.GONE);
        }

        if (pet.getAvatarUrl() != null && !pet.getAvatarUrl().isEmpty()) {
            ImageMultipartUtils.loadCircularAvatar(context, pet.getAvatarUrl(), holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        boolean isExpanded = petId != null && Objects.equals(expandedPetId, petId);
        holder.layoutActions.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        final float[] downPos = new float[2];
        final boolean[] holdTriggered = new boolean[1];

        // Nhấn giữ (đè) khoảng 1 giây trên cảm ứng -> bật/tắt tab nút bên phải
        holder.itemView.setOnTouchListener((v, event) -> {
            if (petId == null) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                cancelPendingHover();
                cancelPendingHold();
                downPos[0] = event.getX();
                downPos[1] = event.getY();
                holdTriggered[0] = false;
                pendingHoldRunnable = () -> {
                    holdTriggered[0] = true;
                    pendingHoldRunnable = null;
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    if (Objects.equals(expandedPetId, petId)) {
                        updateExpandedPet(null, false);
                    } else {
                        updateExpandedPet(petId, true);
                    }
                };
                actionHandler.postDelayed(pendingHoldRunnable, ACTION_SHOW_DELAY_MS);
                return false;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (pendingHoldRunnable != null
                        && (Math.abs(event.getX() - downPos[0]) > touchSlop
                        || Math.abs(event.getY() - downPos[1]) > touchSlop)) {
                    cancelPendingHold();
                }
                return false;
            } else if (action == MotionEvent.ACTION_UP) {
                cancelPendingHold();
                return holdTriggered[0];
            } else if (action == MotionEvent.ACTION_CANCEL) {
                cancelPendingHold();
                return false;
            }
            return false;
        });

        // Bấm nhẹ vào thẻ thú cưng -> mở Trang Chi tiết (giống UI Demo)
        holder.itemView.setOnClickListener(v -> {
            if (holdTriggered[0]) {
                holdTriggered[0] = false;
                return;
            }
            if (listener != null) {
                listener.onPetClick(pet);
            }
        });

        // Hover chuột liên tục ~1 giây -> hiện tab nút bên phải thông tin pet
        holder.itemView.setOnHoverListener((v, event) -> {
            if (petId == null) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE) {
                if (!Objects.equals(expandedPetId, petId) && !Objects.equals(pendingHoverPetId, petId)) {
                    scheduleHoverExpand(petId);
                }
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                boolean leftCardBounds = event.getX() < 0
                        || event.getX() >= v.getWidth()
                        || event.getY() < 0
                        || event.getY() >= v.getHeight();
                if (leftCardBounds) {
                    if (Objects.equals(pendingHoverPetId, petId)) {
                        cancelPendingHover();
                    }
                    if (!expandedByTouch && Objects.equals(expandedPetId, petId)) {
                        updateExpandedPet(null, false);
                    }
                }
            }
            return false;
        });

        holder.btnWeight.setOnClickListener(v -> {
            if (listener != null) listener.onWeightClick(pet);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(pet);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(pet);
        });
    }

    private void scheduleHoverExpand(Long petId) {
        cancelPendingHover();
        pendingHoverPetId = petId;
        pendingHoverRunnable = () -> {
            pendingHoverPetId = null;
            pendingHoverRunnable = null;
            if (findPositionByPetId(petId) != -1) {
                updateExpandedPet(petId, false);
            }
        };
        actionHandler.postDelayed(pendingHoverRunnable, ACTION_SHOW_DELAY_MS);
    }

    private void cancelPendingHover() {
        if (pendingHoverRunnable != null) {
            actionHandler.removeCallbacks(pendingHoverRunnable);
            pendingHoverRunnable = null;
        }
        pendingHoverPetId = null;
    }

    private void cancelPendingHold() {
        if (pendingHoldRunnable != null) {
            actionHandler.removeCallbacks(pendingHoldRunnable);
            pendingHoldRunnable = null;
        }
    }

    private void updateExpandedPet(Long newExpandedPetId, boolean byTouch) {
        Long oldExpandedPetId = expandedPetId;
        expandedPetId = newExpandedPetId;
        expandedByTouch = newExpandedPetId != null && byTouch;

        if (oldExpandedPetId != null && !Objects.equals(oldExpandedPetId, newExpandedPetId)) {
            int oldPos = findPositionByPetId(oldExpandedPetId);
            if (oldPos != -1) {
                notifyItemChanged(oldPos);
            }
        }
        if (newExpandedPetId != null) {
            int newPos = findPositionByPetId(newExpandedPetId);
            if (newPos != -1) {
                notifyItemChanged(newPos);
            }
        }
    }

    private int findPositionByPetId(Long petId) {
        if (petId == null) return -1;
        for (int i = 0; i < petList.size(); i++) {
            if (Objects.equals(petList.get(i).getId(), petId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivAvatar;
        final TextView tvName;
        final TextView tvWeightBadge;
        final TextView tvSpeciesBreed;
        final TextView tvBirthDate;
        final TextView tvOwner;
        final LinearLayout layoutActions;
        final MaterialButton btnWeight;
        final MaterialButton btnEdit;
        final MaterialButton btnDelete;

        PetViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivItemPetAvatar);
            tvName = itemView.findViewById(R.id.tvItemPetName);
            tvWeightBadge = itemView.findViewById(R.id.tvItemPetWeightBadge);
            tvSpeciesBreed = itemView.findViewById(R.id.tvItemPetSpeciesBreed);
            tvBirthDate = itemView.findViewById(R.id.tvItemPetBirthDate);
            tvOwner = itemView.findViewById(R.id.tvItemPetOwner);
            layoutActions = itemView.findViewById(R.id.layoutItemPetActions);
            btnWeight = itemView.findViewById(R.id.btnItemPetWeight);
            btnEdit = itemView.findViewById(R.id.btnItemPetEdit);
            btnDelete = itemView.findViewById(R.id.btnItemPetDelete);
        }
    }
}
