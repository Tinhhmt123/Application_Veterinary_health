package com.example.healthy_pet.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.healthy_pet.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Tiện ích dùng chung cho cả 5 thành viên để:
 * 1. Đóng gói ảnh chụp/chọn từ máy thành MultipartBody.Part gửi qua Retrofit (@Multipart) lên Spring Boot -> Cloudinary.
 * 2. Hiển thị ảnh từ URL Cloudinary (avatar_url, stamp_image_url, prescription_image_url, image_url) bằng thư viện Glide.
 */
public class ImageMultipartUtils {

    /**
     * Chuyển chuỗi Text sang RequestBody dùng chung với @Multipart
     */
    public static RequestBody toTextRequestBody(String value) {
        return RequestBody.create(
                MediaType.parse("text/plain; charset=utf-8"),
                value != null ? value : ""
        );
    }

    /**
     * Chuyển Uri ảnh (từ Camera hoặc Thư viện ảnh Android) sang MultipartBody.Part
     * để gửi qua Retrofit @Multipart lên Spring Boot đẩy vào Cloudinary.
     *
     * @param context   Context
     * @param imageUri  Uri của ảnh vừa chọn/chụp
     * @param partName  Tên tham số @Part trên API (VD: "avatarFile", "avatarImage", "stampImage", "prescriptionImage", "clinicImage")
     */
    public static MultipartBody.Part uriToMultipart(Context context, Uri imageUri, String partName) throws IOException {
        if (imageUri == null) {
            return null;
        }
        String fileName = getFileName(context, imageUri);
        File tempFile = new File(context.getCacheDir(), fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), tempFile);
        return MultipartBody.Part.createFormData(partName, tempFile.getName(), requestFile);
    }

    /**
     * Hiển thị ảnh từ URL Cloudinary lên ImageView bằng Glide (Bo góc vuông mềm)
     */
    public static void loadCloudinaryImage(Context context, String cloudinaryUrl, ImageView targetImageView) {
        Glide.with(context)
                .load(cloudinaryUrl)
                .transform(new RoundedCorners(24))
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(targetImageView);
    }

    /**
     * Hiển thị Avatar tròn từ URL Cloudinary lên ImageView bằng Glide (Dùng cho User & Pet Avatar)
     */
    public static void loadCircularAvatar(Context context, String cloudinaryUrl, ImageView targetImageView) {
        Glide.with(context)
                .load(cloudinaryUrl)
                .transform(new CircleCrop())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(targetImageView);
    }

    private static String getFileName(Context context, Uri uri) {
        String result = "upload_" + System.currentTimeMillis() + ".jpg";
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        return result;
    }
}
