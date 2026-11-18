package com.example.healthy_pet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.WeightLog;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị từng mốc Nhật ký cân nặng trong hộp thoại WeightLogs (gắn với item_weight_log_admin_user.xml).
 */
public class Weight_Log_Adapter_Admin_User extends RecyclerView.Adapter<Weight_Log_Adapter_Admin_User.WeightViewHolder> {

    public interface OnWeightLogActionListener {
        void onEditLog(WeightLog log);
        void onDeleteLog(WeightLog log);
    }

    private final List<WeightLog> logs = new ArrayList<>();
    private final OnWeightLogActionListener listener;

    public Weight_Log_Adapter_Admin_User(OnWeightLogActionListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<WeightLog> newLogs) {
        logs.clear();
        if (newLogs != null) {
            logs.addAll(newLogs);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weight_log_admin_user, parent, false);
        return new WeightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        WeightLog log = logs.get(position);
        double weight = log.getWeight() != null ? log.getWeight() : 0.0;
        holder.tvWeightValue.setText(String.format(Locale.US, "%.1f kg", weight));
        holder.tvWeightDate.setText(log.getRecordDate() != null ? log.getRecordDate() : "");

        if (log.getNote() != null && !log.getNote().trim().isEmpty()) {
            holder.tvWeightNote.setText(log.getNote().trim());
            holder.tvWeightNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvWeightNote.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditLog(log);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteLog(log);
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class WeightViewHolder extends RecyclerView.ViewHolder {
        final TextView tvWeightValue;
        final TextView tvWeightDate;
        final TextView tvWeightNote;
        final MaterialButton btnEdit;
        final MaterialButton btnDelete;

        WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWeightValue = itemView.findViewById(R.id.tvItemWeightValue);
            tvWeightDate = itemView.findViewById(R.id.tvItemWeightDate);
            tvWeightNote = itemView.findViewById(R.id.tvItemWeightNote);
            btnEdit = itemView.findViewById(R.id.btnItemWeightEdit);
            btnDelete = itemView.findViewById(R.id.btnItemWeightDelete);
        }
    }
}
