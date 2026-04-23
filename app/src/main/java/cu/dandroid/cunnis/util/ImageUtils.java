package cu.dandroid.cunnis.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ImageUtils {
    private ImageUtils() {}

    public static byte[] compressBitmap(Bitmap bitmap, int maxWidth, int maxHeight, int quality) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale = Math.min(
            (float) maxWidth / width,
            (float) maxHeight / height
        );

        if (scale >= 1.0f) {
            // No scaling needed, just compress
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            return stream.toByteArray();
        }

        Bitmap scaled = Bitmap.createScaledBitmap(
            bitmap,
            (int) (width * scale),
            (int) (height * scale),
            true
        );

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream);

        if (scaled != bitmap) {
            scaled.recycle();
        }

        return stream.toByteArray();
    }

    public static Bitmap bytesToBitmap(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static byte[] compressBitmap(Bitmap bitmap) {
        return compressBitmap(bitmap, Constants.MAX_PHOTO_WIDTH, Constants.MAX_PHOTO_HEIGHT, Constants.PHOTO_QUALITY);
    }

    public static File saveBitmapToFile(Context context, Bitmap bitmap, String filename) throws IOException {
        File file = new File(context.getExternalFilesDir("Pictures"), filename);
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.PHOTO_QUALITY, fos);
        fos.flush();
        fos.close();
        return file;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
