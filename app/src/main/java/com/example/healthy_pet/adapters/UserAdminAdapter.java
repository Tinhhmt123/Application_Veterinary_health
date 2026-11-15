package com.example.healthy_pet.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.User;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách khách hàng cho Quản trị viên (gắn với item_user_admin.xml).
 * Hỗ trợ tìm kiếm và bấm nút Khóa / Mở khóa tài khoản (is_active).
 */
public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.UserViewHolder> {

    public interface OnUserLockClickListener {
        void onToggleLockClick(User user, int position);
    }

    private final List<User> userList = new ArrayList<>();
    private final OnUserLockClickListener lockClickListener;

    public UserAdminAdapter(OnUserLockClickListener lockClickListener) {
        this.lockClickListener = lockClickListener;
    }

    public void setUsers(List<User> users) {
        userList.clear();
        if (users != null) {
            userList.addAll(users);
        }
        notifyDataSetChanged();
    }

    public void updateUserAt(int position, User updatedUser) {
        if (position >= 0 && position < userList.size() && updatedUser != null) {
            userList.set(position, updatedUser);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvItemUserFullName.setText(user.getFullName() != null ? user.getFullName() : "Chưa cập nhật");
        holder.tvItemUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        String phone = (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty())
                ? user.getPhoneNumber()
                : "Chưa có SĐT";
        holder.tvItemUserPhone.setText("SĐT: " + phone);

        // Hiển thị Avatar tròn từ Cloudinary
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ImageMultipartUtils.loadCircularAvatar(context, user.getAvatarUrl(), holder.ivItemUserAvatar);
        } else {
            holder.ivItemUserAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        holder.tvItemUserRole.setText(isAdmin ? "ADMIN" : "Khách hàng");

        boolean isActive = user.getIsActive() == null || Boolean.TRUE.equals(user.getIsActive());
        int greenColor = ContextCompat.getColor(context, R.color.emerald_600);
        int greenBg = ContextCompat.getColor(context, R.color.emerald_100);
        int redColor = ContextCompat.getColor(context, R.color.red_600);

        if (isActive) {
            holder.tvItemUserStatus.setText("Đang hoạt động");
            holder.tvItemUserStatus.setTextColor(ContextCompat.getColor(context, R.color.emerald_700));
            holder.tvItemUserStatus.setBackgroundColor(greenBg);

            holder.btnToggleUserLock.setText("🔒 Khóa");
            holder.btnToggleUserLock.setTextColor(redColor);
            holder.btnToggleUserLock.setStrokeColor(ColorStateList.valueOf(redColor));
        } else {
            holder.tvItemUserStatus.setText("Đã bị khóa");
            holder.tvItemUserStatus.setTextColor(redColor);
            holder.tvItemUserStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.slate_100));

            holder.btnToggleUserLock.setText("🔓 Mở khóa");
            holder.btnToggleUserLock.setTextColor(greenColor);
            holder.btnToggleUserLock.setStrokeColor(ColorStateList.valueOf(greenColor));
        }

        // Không cho phép khóa tài khoản có quyền ADMIN
        if (isAdmin) {
            holder.btnToggleUserLock.setVisibility(View.INVISIBLE);
            holder.btnToggleUserLock.setOnClickListener(null);
        } else {
            holder.btnToggleUserLock.setVisibility(View.VISIBLE);
            holder.btnToggleUserLock.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION && lockClickListener != null) {
                    lockClickListener.onToggleLockClick(userList.get(currentPos), currentPos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivItemUserAvatar;
        final TextView tvItemUserFullName;
        final TextView tvItemUserEmail;
        final TextView tvItemUserPhone;
        final TextView tvItemUserRole;
        final TextView tvItemUserStatus;
        final MaterialButton btnToggleUserLock;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemUserAvatar = itemView.findViewById(R.id.ivItemUserAvatar);
            tvItemUserFullName = itemView.findViewById(R.id.tvItemUserFullName);
            tvItemUserEmail = itemView.findViewById(R.id.tvItemUserEmail);
            tvItemUserPhone = itemView.findViewById(R.id.tvItemUserPhone);
            tvItemUserRole = itemView.findViewById(R.id.tvItemUserRole);
            tvItemUserStatus = itemView.findViewById(R.id.tvItemUserStatus);
            btnToggleUserLock = itemView.findViewById(R.id.btnToggleUserLock);
        }
    }
}
