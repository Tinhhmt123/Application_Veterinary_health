package com.example.healthy_pet.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Retrofit Singleton dùng chung cho cả 5 thành viên kết nối tới Spring Boot Web API.
 * Mặc định sử dụng 10.0.2.2:8080 cho máy ảo Android (Android Emulator).
 */
public class RetrofitClient {
    public static final String BASE_URL = "http://10.0.2.2:8080/api/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthUserApi getAuthUserApi() {
        return getClient().create(AuthUserApi.class);
    }

    public static PetWeightApi getPetWeightApi() {
        return getClient().create(PetWeightApi.class);
    }

    public static VaccinationApi getVaccinationApi() {
        return getClient().create(VaccinationApi.class);
    }

    public static ClinicMapApi getClinicMapApi() {
        return getClient().create(ClinicMapApi.class);
    }

    public static AppointmentMedicalApi getAppointmentMedicalApi() {
        return getClient().create(AppointmentMedicalApi.class);
    }
}
