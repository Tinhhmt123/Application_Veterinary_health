package com.example.healthy_pet.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.ClinicAdapter;
import com.example.healthy_pet.api.ClinicMapApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.VetClinic;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminClinicActivity extends AppCompatActivity implements ClinicAdapter.OnClinicActionListener {

    private RecyclerView rvClinics;
    private ClinicAdapter adapter;
    private List<VetClinic> clinicList = new ArrayList<>();
    private ClinicMapApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_clinic);

        rvClinics = findViewById(R.id.rvAdminClinics);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddClinic);

        api = RetrofitClient.getClient().create(ClinicMapApi.class);

        rvClinics.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClinicAdapter(clinicList, this);
        rvClinics.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddEditDialog(null));

        loadClinics();
    }

    private void loadClinics() {
        api.getAllClinics().enqueue(new Callback<List<VetClinic>>() {
            @Override
            public void onResponse(Call<List<VetClinic>> call, Response<List<VetClinic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    clinicList.clear();
                    clinicList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<VetClinic>> call, Throwable t) {
                Toast.makeText(AdminClinicActivity.this, "Lỗi tải danh sách chi nhánh!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RequestBody toRequestBody(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value != null ? value : "");
    }

    private void showAddEditDialog(VetClinic clinicToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_clinic, null);

        EditText edtName = view.findViewById(R.id.edtName);
        EditText edtAddress = view.findViewById(R.id.edtAddress);
        EditText edtPhone = view.findViewById(R.id.edtPhone);
        EditText edtLat = view.findViewById(R.id.edtLatitude);
        EditText edtLng = view.findViewById(R.id.edtLongitude);
        EditText edtHours = view.findViewById(R.id.edtOpenHours);

        boolean isEdit = (clinicToEdit != null);
        if (isEdit) {
            edtName.setText(clinicToEdit.getName());
            edtAddress.setText(clinicToEdit.getAddress());
            edtPhone.setText(clinicToEdit.getPhoneNumber());
            edtLat.setText(String.valueOf(clinicToEdit.getLatitude()));
            edtLng.setText(String.valueOf(clinicToEdit.getLongitude()));
            edtHours.setText(clinicToEdit.getOpenHours());
        }

        builder.setTitle(isEdit ? "Cập nhật Chi nhánh" : "Thêm Chi nhánh mới")
                .setView(view)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String address = edtAddress.getText().toString().trim();
                    String phone = edtPhone.getText().toString().trim();
                    String latStr = edtLat.getText().toString().trim();
                    String lngStr = edtLng.getText().toString().trim();
                    String hours = edtHours.getText().toString().trim();

                    if (name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập tên và tọa độ!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RequestBody reqName = toRequestBody(name);
                    RequestBody reqAddress = toRequestBody(address);
                    RequestBody reqPhone = toRequestBody(phone);
                    RequestBody reqLat = toRequestBody(latStr);
                    RequestBody reqLng = toRequestBody(lngStr);
                    RequestBody reqHours = toRequestBody(hours);

                    if (isEdit) {
                        api.updateClinic(clinicToEdit.getId(), reqName, reqAddress, reqPhone, reqLat, reqLng, reqHours, null)
                                .enqueue(new Callback<VetClinic>() {
                                    @Override
                                    public void onResponse(Call<VetClinic> call, Response<VetClinic> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(AdminClinicActivity.this, "Cập nhật chi nhánh thành công", Toast.LENGTH_SHORT).show();
                                            loadClinics();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<VetClinic> call, Throwable t) {
                                        Toast.makeText(AdminClinicActivity.this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        api.createClinic(reqName, reqAddress, reqPhone, reqLat, reqLng, reqHours, null)
                                .enqueue(new Callback<VetClinic>() {
                                    @Override
                                    public void onResponse(Call<VetClinic> call, Response<VetClinic> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(AdminClinicActivity.this, "Thêm chi nhánh mới thành công", Toast.LENGTH_SHORT).show();
                                            loadClinics();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<VetClinic> call, Throwable t) {
                                        Toast.makeText(AdminClinicActivity.this, "Lỗi thêm chi nhánh!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onEdit(VetClinic clinic) {
        showAddEditDialog(clinic);
    }

    @Override
    public void onDelete(VetClinic clinic) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Chi nhánh")
                .setMessage("Bạn chắc chắn muốn xóa chi nhánh " + clinic.getName() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    api.deleteClinic(clinic.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminClinicActivity.this, "Đã xóa chi nhánh", Toast.LENGTH_SHORT).show();
                                loadClinics();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(AdminClinicActivity.this, "Lỗi khi xóa chi nhánh!", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}