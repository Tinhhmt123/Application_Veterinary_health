package com.example.healthy_pet.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
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

    private static final int MAX_IMAGE_DIMENSION = 800;
    private static final int JPEG_QUALITY = 80;

    /**
     * Chuyển Uri ảnh (từ Camera hoặc Thư viện ảnh Android) sang MultipartBody.Part
     * (tự động thu nhỏ tối đa 800px và nén JPEG 80% để giảm dung lượng, xóa EXIF và upload nhanh hơn).
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
        byte[] compressedBytes = decodeResizeAndCompress(context, imageUri);
        if (compressedBytes != null) {
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), compressedBytes);
            return MultipartBody.Part.createFormData(partName, ensureJpgFileName(fileName), requestBody);
        }

        // Fallback nếu không decode được bitmap (giữ nguyên dữ liệu gốc)
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

    private static byte[] decodeResizeAndCompress(Context context, Uri imageUri) throws IOException {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        try (InputStream boundsStream = context.getContentResolver().openInputStream(imageUri)) {
            if (boundsStream == null) {
                return null;
            }
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        }

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = calculateInSampleSize(
                boundsOptions.outWidth, boundsOptions.outHeight, MAX_IMAGE_DIMENSION);

        Bitmap decodedBitmap;
        try (InputStream decodeStream = context.getContentResolver().openInputStream(imageUri)) {
            if (decodeStream == null) {
                return null;
            }
            decodedBitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
        }
        if (decodedBitmap == null) {
            return null;
        }

        Bitmap orientedBitmap = applyExifOrientation(context, imageUri, decodedBitmap);
        Bitmap finalBitmap = scaleDownIfNeeded(orientedBitmap, MAX_IMAGE_DIMENSION);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            return outputStream.toByteArray();
        } finally {
            if (!finalBitmap.isRecycled()) {
                finalBitmap.recycle();
            }
        }
    }

    private static int calculateInSampleSize(int width, int height, int maxDimension) {
        int inSampleSize = 1;
        int longestSide = Math.max(width, height);
        while (longestSide / (inSampleSize * 2) >= maxDimension) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    private static Bitmap applyExifOrientation(Context context, Uri imageUri, Bitmap bitmap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return bitmap;
        }
        try (InputStream exifStream = context.getContentResolver().openInputStream(imageUri)) {
            if (exifStream == null) {
                return bitmap;
            }
            ExifInterface exif = new ExifInterface(exifStream);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270f);
                    break;
                default:
                    return bitmap;
            }
            Bitmap rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (IOException ignored) {
            return bitmap;
        }
    }

    private static Bitmap scaleDownIfNeeded(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxDimension) {
            return bitmap;
        }
        float scale = (float) maxDimension / longestSide;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        if (scaled != bitmap) {
            bitmap.recycle();
        }
        return scaled;
    }

    private static String ensureJpgFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        return baseName + ".jpg";
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
