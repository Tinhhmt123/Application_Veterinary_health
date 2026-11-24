package com.example.healthy_pet.adapters;

import android.content.res.Configuration;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.VetClinic;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter hiển thị thẻ chi nhánh chi tiết dành cho User (dùng cho cả Top 5 sổ xuống
 * và Thẻ lớn trượt xem tất cả chi nhánh).
 * Thống nhất 1 tông màu xanh ngọc (Emerald), làm nổi bật rõ viền & nền thẻ đang chọn,
 * tương thích hoàn toàn cả Light Mode và Dark Mode.
 */
public class UserClinicCardAdapter extends RecyclerView.Adapter<UserClinicCardAdapter.ViewHolder> {

    public interface OnUserClinicCardListener {
        void onSelectClinicOnMap(VetClinic clinic);
        void onCallClinic(VetClinic clinic);
        void onBookClinic(VetClinic clinic);
    }

    private final List<VetClinic> clinics = new ArrayList<>();
    private final Map<Long, Integer> rankByClinicId = new HashMap<>();
    private final OnUserClinicCardListener listener;
    private Long selectedClinicId;

    public UserClinicCardAdapter(OnUserClinicCardListener listener) {
        this.listener = listener;
    }

    public void setData(@Nullable List<VetClinic> items,
                        @Nullable Map<Long, Integer> rankMap,
                        @Nullable Long selectedId) {
        clinics.clear();
        if (items != null) {
            clinics.addAll(items);
        }
        rankByClinicId.clear();
        if (rankMap != null) {
            rankByClinicId.putAll(rankMap);
        }
        this.selectedClinicId = selectedId;
        notifyDataSetChanged();
    }

    public void setSelectedClinicId(@Nullable Long selectedId) {
        this.selectedClinicId = selectedId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clinic_user_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VetClinic clinic = clinics.get(position);
        boolean isNight = (holder.itemView.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        boolean isSelected = selectedClinicId != null
                && clinic.getId() != null
                && selectedClinicId.equals(clinic.getId());

        float density = holder.itemView.getResources().getDisplayMetrics().density;

        // Xác định thứ hạng chi nhánh để hiển thị Badge phân biệt rõ ràng
        int rank = (clinic.getId() != null && rankByClinicId.containsKey(clinic.getId()))
                ? rankByClinicId.get(clinic.getId())
                : (position + 1);

        if (rank == 1) {
            holder.tvRankBadge.setText("🏆 Top 1 Yêu thích");
        } else if (rank <= 5) {
            holder.tvRankBadge.setText("⭐ Top " + rank + " Yêu thích");
        } else {
            holder.tvRankBadge.setText("🏥 Chi nhánh #" + rank);
        }
        holder.tvRankBadge.setTextColor(Color.parseColor(isNight ? "#6EE7B7" : "#047857"));

        // Trạng thái thẻ đang được chọn trên bản đồ
        if (isSelected) {
            holder.tvSelectedBadge.setVisibility(View.VISIBLE);
            holder.tvSelectedBadge.setTextColor(Color.parseColor(isNight ? "#A5B4FC" : "#4F46E5"));
            holder.cardRoot.setStrokeWidth((int) (2.2f * density));
            holder.cardRoot.setStrokeColor(Color.parseColor("#10B981"));
            holder.cardRoot.setCardBackgroundColor(Color.parseColor(isNight ? "#16302B" : "#F0FDF4"));
        } else {
            holder.tvSelectedBadge.setVisibility(View.GONE);
            holder.cardRoot.setStrokeWidth((int) (1.2f * density));
            holder.cardRoot.setStrokeColor(Color.parseColor(isNight ? "#3394A3B8" : "#2664748B"));
            holder.cardRoot.setCardBackgroundColor(Color.parseColor(isNight ? "#1E293B" : "#FFFFFF"));
        }

        String name = clinic.getName() != null && !clinic.getName().trim().isEmpty()
                ? clinic.getName().trim()
                : "Chi nhánh Healthy Pet";
        String address = clinic.getAddress() != null && !clinic.getAddress().trim().isEmpty()
                ? clinic.getAddress().trim()
                : "Đang cập nhật địa chỉ";
        String hours = clinic.getOpenHours() != null && !clinic.getOpenHours().trim().isEmpty()
                ? clinic.getOpenHours().trim()
                : "07:00 - 17:00";
        String phone = clinic.getPhoneNumber() != null && !clinic.getPhoneNumber().trim().isEmpty()
                ? clinic.getPhoneNumber().trim()
                : "Chưa có số";

        holder.tvName.setText(name);
        holder.tvAddress.setText("📍 " + address);
        holder.tvHours.setText("🕒 " + hours);
        holder.tvPhone.setText("📞 " + phone);
        holder.tvPhone.setTextColor(Color.parseColor(isNight ? "#34D399" : "#059669"));

        if (clinic.getImageUrl() != null && !clinic.getImageUrl().trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(clinic.getImageUrl().trim())
                    .placeholder(R.drawable.ic_clinic_building)
                    .error(R.drawable.ic_clinic_building)
                    .into(holder.ivThumb);
        } else {
            holder.ivThumb.setImageResource(R.drawable.ic_clinic_building);
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) listener.onSelectClinicOnMap(clinic);
        });
        holder.btnCall.setOnClickListener(v -> {
            if (listener != null) listener.onCallClinic(clinic);
        });
        holder.btnBook.setOnClickListener(v -> {
            if (listener != null) listener.onBookClinic(clinic);
        });
    }

    @Override
    public int getItemCount() {
        return clinics.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final ImageView ivThumb;
        final TextView tvRankBadge;
        final TextView tvSelectedBadge;
        final TextView tvName;
        final TextView tvAddress;
        final TextView tvHours;
        final TextView tvPhone;
        final MaterialButton btnCall;
        final MaterialButton btnBook;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardClinicRoot);
            ivThumb = itemView.findViewById(R.id.ivClinicThumb);
            tvRankBadge = itemView.findViewById(R.id.tvClinicRankBadge);
            tvSelectedBadge = itemView.findViewById(R.id.tvClinicSelectedBadge);
            tvName = itemView.findViewById(R.id.tvClinicCardName);
            tvAddress = itemView.findViewById(R.id.tvClinicCardAddress);
            tvHours = itemView.findViewById(R.id.tvClinicCardHours);
            tvPhone = itemView.findViewById(R.id.tvClinicCardPhone);
            btnCall = itemView.findViewById(R.id.btnCardCallClinic);
            btnBook = itemView.findViewById(R.id.btnCardBookClinic);
        }
    }
}
