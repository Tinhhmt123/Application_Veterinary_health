package com.example.healthy_pet.ui.map;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.R;
import com.example.healthy_pet.api.ClinicMapApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.VetClinic;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClinicBookingFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MaterialButton btnModuleAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clinic_booking, container, false);

        // Khởi tạo Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Nút đặt lịch hẹn (Nghiệp vụ phối hợp với Thành viên 5)
        btnModuleAction = view.findViewById(R.id.btnModuleAction);
        if (btnModuleAction != null) {
            btnModuleAction.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Tạo lịch hẹn mới (Trạng thái: PENDING)", Toast.LENGTH_SHORT).show()
            );
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        loadClinicsOnMap();
    }

    private void loadClinicsOnMap() {
        ClinicMapApi api = RetrofitClient.getClient().create(ClinicMapApi.class);
        api.getAllClinics().enqueue(new Callback<List<VetClinic>>() {
            @Override
            public void onResponse(Call<List<VetClinic>> call, Response<List<VetClinic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VetClinic> clinics = response.body();
                    for (VetClinic clinic : clinics) {
                        if (clinic.getLatitude() != null && clinic.getLongitude() != null) {
                            LatLng pos = new LatLng(clinic.getLatitude(), clinic.getLongitude());

                            String snippetInfo = "📞 Hotline: " + (clinic.getPhoneNumber() != null ? clinic.getPhoneNumber() : "Chưa có")
                                    + " | 🕒 Giờ mở: " + (clinic.getOpenHours() != null ? clinic.getOpenHours() : "8:00 - 20:00");

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(clinic.getName())
                                    .snippet(snippetInfo)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                            if (marker != null) {
                                marker.setTag(clinic);
                            }
                        }
                    }

                    if (!clinics.isEmpty() && clinics.get(0).getLatitude() != null) {
                        VetClinic first = clinics.get(0);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(first.getLatitude(), first.getLongitude()), 13f));
                    }
                }
            }

            @Override
            public void onFailure(Call<List<VetClinic>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể kết nối lấy dữ liệu phòng khám!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Xử lý sự kiện bấm vào InfoWindow của ghim để gọi Hotline trực tiếp (ACTION_DIAL)
        mMap.setOnInfoWindowClickListener(marker -> {
            VetClinic clinic = (VetClinic) marker.getTag();
            if (clinic != null && clinic.getPhoneNumber() != null && !clinic.getPhoneNumber().isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + clinic.getPhoneNumber()));
                startActivity(dialIntent);
            } else {
                Toast.makeText(getContext(), "Chi nhánh này chưa cập nhật số điện thoại Hotline", Toast.LENGTH_SHORT).show();
            }
        });
    }
}