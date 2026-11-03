package com.example.healthy_pet.api;

import com.example.healthy_pet.models.VetClinic;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * [Thành viên 4] API Bản đồ Phòng khám (NV 5a) & Admin Quản lý Chi nhánh VetClinics (NV 8)
 */
public interface ClinicMapApi {

    @GET("clinics")
    Call<List<VetClinic>> getAllClinics();

    @Multipart
    @POST("admin/clinics")
    Call<VetClinic> createClinic(
            @Part("name") RequestBody name,
            @Part("address") RequestBody address,
            @Part("phone_number") RequestBody phoneNumber,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("open_hours") RequestBody openHours,
            @Part MultipartBody.Part clinicImage
    );

    @Multipart
    @PUT("admin/clinics/{id}")
    Call<VetClinic> updateClinic(
            @Path("id") Long clinicId,
            @Part("name") RequestBody name,
            @Part("address") RequestBody address,
            @Part("phone_number") RequestBody phoneNumber,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("open_hours") RequestBody openHours,
            @Part MultipartBody.Part clinicImage
    );

    @DELETE("admin/clinics/{id}")
    Call<Void> deleteClinic(@Path("id") Long clinicId);
}
