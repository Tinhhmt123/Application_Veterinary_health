package com.example.healthy_pet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.VetClinic;
import java.util.List;

public class ClinicAdapter extends RecyclerView.Adapter<ClinicAdapter.ClinicViewHolder> {

    public interface OnClinicActionListener {
        void onEdit(VetClinic clinic);
        void onDelete(VetClinic clinic);
    }

    private List<VetClinic> clinicList;
    private OnClinicActionListener listener;

    public ClinicAdapter(List<VetClinic> clinicList, OnClinicActionListener listener) {
        this.clinicList = clinicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClinicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clinic_admin, parent, false);
        return new ClinicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClinicViewHolder holder, int position) {
        VetClinic clinic = clinicList.get(position);

        holder.tvName.setText(clinic.getName());
        holder.tvAddress.setText("Địa chỉ: " + clinic.getAddress());
        holder.tvPhone.setText("Hotline: " + clinic.getPhoneNumber());
        holder.tvOpenHours.setText("Giờ mở cửa: " + clinic.getOpenHours());
        holder.tvLatLng.setText("Tọa độ: " + clinic.getLatitude() + ", " + clinic.getLongitude());

        if (clinic.getImageUrl() != null && !clinic.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(clinic.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background) // Ảnh mặc định nếu đang tải
                    .into(holder.imgClinic);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(clinic));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(clinic));
    }

    @Override
    public int getItemCount() {
        return clinicList != null ? clinicList.size() : 0;
    }

    static class ClinicViewHolder extends RecyclerView.ViewHolder {
        ImageView imgClinic;
        TextView tvName, tvAddress, tvPhone, tvOpenHours, tvLatLng;
        ImageButton btnEdit, btnDelete;

        public ClinicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgClinic = itemView.findViewById(R.id.imgClinic);
            tvName = itemView.findViewById(R.id.tvClinicName);
            tvAddress = itemView.findViewById(R.id.tvClinicAddress);
            tvPhone = itemView.findViewById(R.id.tvClinicPhone);
            tvOpenHours = itemView.findViewById(R.id.tvClinicOpenHours);
            tvLatLng = itemView.findViewById(R.id.tvClinicLatLng);
            btnEdit = itemView.findViewById(R.id.btnEditClinic);
            btnDelete = itemView.findViewById(R.id.btnDeleteClinic);
        }
    }
}