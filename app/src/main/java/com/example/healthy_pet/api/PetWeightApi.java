package com.example.healthy_pet.api;

import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.models.WeightLog;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * [Thành viên 2] API Hồ sơ Thú cưng & Nhật ký cân nặng (NV 2 & NV 6b)
 */
public interface PetWeightApi {

    @GET("pets")
    Call<List<Pet>> getPetsByUser(@Query("userId") Long userId);

    @GET("pets/{id}")
    Call<Pet> getPetById(@Path("id") Long petId);

    @Multipart
    @POST("pets")
    Call<Pet> createPet(
            @Part("user_id") RequestBody userId,
            @Part("name") RequestBody name,
            @Part("species") RequestBody species,
            @Part("breed") RequestBody breed,
            @Part("gender") RequestBody gender,
            @Part("birth_date") RequestBody birthDate,
            @Part("current_weight") RequestBody currentWeight,
            @Part MultipartBody.Part avatarImage
    );

    @Multipart
    @PUT("pets/{id}")
    Call<Pet> updatePet(
            @Path("id") Long petId,
            @Part("user_id") RequestBody userId,
            @Part("name") RequestBody name,
            @Part("species") RequestBody species,
            @Part("breed") RequestBody breed,
            @Part("gender") RequestBody gender,
            @Part("birth_date") RequestBody birthDate,
            @Part("current_weight") RequestBody currentWeight,
            @Part MultipartBody.Part avatarImage
    );

    @DELETE("pets/{id}")
    Call<Void> deletePet(@Path("id") Long petId);

    // Nhật ký cân nặng (WeightLogs) - Tự đồng bộ current_weight
    @GET("weight-logs")
    Call<List<WeightLog>> getWeightLogsByPet(@Query("petId") Long petId);

    @POST("weight-logs")
    Call<WeightLog> addWeightLog(@Body WeightLog weightLog);

    @PUT("weight-logs/{id}")
    Call<WeightLog> updateWeightLog(@Path("id") Long logId, @Body WeightLog weightLog);

    @DELETE("weight-logs/{id}")
    Call<Void> deleteWeightLog(@Path("id") Long logId);

    // Admin: Tra cứu toàn bộ thú cưng tại quầy
    @GET("admin/pets")
    Call<List<Pet>> searchAllPets(
            @Query("keyword") String keyword,
            @Query("species") String species,
            @Query("userId") Long userId
    );
}
