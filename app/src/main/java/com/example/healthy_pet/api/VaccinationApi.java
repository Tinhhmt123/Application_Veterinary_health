package com.example.healthy_pet.api;

import com.example.healthy_pet.models.VaccinationRecord;

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
import retrofit2.http.Query;

public interface VaccinationApi {

    @GET("vaccinations/alerts")
    Call<List<VaccinationRecord>> getHealthAlerts();

    @GET("vaccinations")
    Call<List<VaccinationRecord>> getVaccinationsByPet(
            @Query("petId") Long petId
    );

    @GET("vaccinations/all")
    Call<List<VaccinationRecord>> getAllVaccinations();

    // ADMIN tạo hồ sơ; stampImage để null vì USER sẽ tải ảnh sau.
    @Multipart
    @POST("vaccinations")
    Call<VaccinationRecord> createVaccination(
            @Part("pet_id") RequestBody petId,
            @Part("vaccine_name") RequestBody vaccineName,
            @Part("injection_date") RequestBody injectionDate,
            @Part("next_due_date") RequestBody nextDueDate,
            @Part("clinic_name") RequestBody clinicName,
            @Part MultipartBody.Part stampImage
    );

    // ADMIN cập nhật thông tin; không gửi ảnh dấu mộc.
    @Multipart
    @PUT("vaccinations/{id}")
    Call<VaccinationRecord> updateVaccination(
            @Path("id") Long id,
            @Part("pet_id") RequestBody petId,
            @Part("vaccine_name") RequestBody vaccineName,
            @Part("injection_date") RequestBody injectionDate,
            @Part("next_due_date") RequestBody nextDueDate,
            @Part("clinic_name") RequestBody clinicName,
            @Part MultipartBody.Part stampImage
    );

    // USER chỉ tải/thay ảnh dấu mộc của hồ sơ đã tồn tại.
    @Multipart
    @POST("vaccinations/{id}/stamp")
    Call<VaccinationRecord> uploadVaccinationStamp(
            @Path("id") Long id,
            @Part MultipartBody.Part stampImage
    );

    @DELETE("vaccinations/{id}")
    Call<Void> deleteVaccination(@Path("id") Long id);
}