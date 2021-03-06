package com.android.volley.toolbox;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.File;

/**
 * Created by andrea on 05/06/17.
 */

public class ImageUtils {

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary      Maximum size of the primary dimension (i.e. width for
     *                        max width), or zero to maintain aspect ratio with secondary
     *                        dimension.
     * @param maxSecondary    Maximum size of the secondary dimension, or zero to
     *                        maintain aspect ratio with primary dimension.
     * @param actualPrimary   Actual size of the primary dimension.
     * @param actualSecondary Actual size of the secondary dimension.
     * @param scaleType       The ScaleType used to calculate the needed image size.
     * @return The resized dimension.
     */
    public static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
                                          int actualSecondary, ImageView.ScaleType scaleType) {

        // If no dominant value at all, just return the actual.
        if ((maxPrimary == 0) && (maxSecondary == 0)) {
            return actualPrimary;
        }

        // If ScaleType.FIT_XY fill the whole rectangle, ignore ratio.
        if (scaleType == ImageView.ScaleType.FIT_XY) {
            if (maxPrimary == 0) {
                return actualPrimary;
            }
            return maxPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;

        // If ScaleType.CENTER_CROP fill the whole rectangle, preserve aspect ratio.
        if (scaleType == ImageView.ScaleType.CENTER_CROP) {
            if ((resized * ratio) < maxSecondary) {
                resized = (int) (maxSecondary / ratio);
            }
            return resized;
        }

        if ((resized * ratio) > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth   Actual width of the bitmap.
     * @param actualHeight  Actual height of the bitmap.
     * @param desiredWidth  Desired width of the bitmap.
     * @param desiredHeight Desired height of the bitmap.
     * @return The best sample size for downscaling.
     */
    // Visible for testing.
    public static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }

    /**
     * Decode a {@link Bitmap} bitmap from the specified byte array.
     *
     * @param data      The byte array of compressed image data.
     * @param maxWidth  Desired width of the bitmap.
     * @param maxHeight Desired height of the bitmap.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap create(byte[] data,
                                int maxWidth, int maxHeight) {
        return create(data, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), ImageView.ScaleType.CENTER_INSIDE);
    }

    /**
     * Decode a {@link Bitmap} bitmap from the specified byte array.
     *
     * @param data      The byte array of compressed image data.
     * @param maxWidth  Desired width of the bitmap.
     * @param maxHeight Desired height of the bitmap.
     * @param scaleType The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap create(byte[] data,
                                int maxWidth, int maxHeight,
                                ImageView.ScaleType scaleType) {
        return create(data, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), scaleType);
    }

    /**
     * Decode a {@link Bitmap} bitmap from the specified byte array.
     *
     * @param data          The byte array of compressed image data.
     * @param maxWidth      Desired width of the bitmap.
     * @param maxHeight     Desired height of the bitmap.
     * @param decodeOptions The Bitmap decode config to be used.
     * @param scaleType     The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap create(byte[] data, int maxWidth, int maxHeight,
                                BitmapFactory.Options decodeOptions,
                                ImageView.ScaleType scaleType) {
        if (maxWidth == 0 && maxHeight == 0) {
            return BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        }

        Bitmap bitmap;
        // If we have to resize this image, first get the natural bounds.
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;

        // Then compute the dimensions we would ideally like to decode to.
        int desiredWidth = getResizedDimension(maxWidth, maxHeight,
                actualWidth, actualHeight, scaleType);
        int desiredHeight = getResizedDimension(maxHeight, maxWidth,
                actualHeight, actualWidth, scaleType);

        // Decode to the nearest power of two scaling factor.
        decodeOptions.inJustDecodeBounds = false;
        decodeOptions.inSampleSize =
                findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
        Bitmap tempBitmap =
                BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

        // If necessary, scale down to the maximal acceptable size.
        if (tempBitmap != null
                && (tempBitmap.getWidth() > desiredWidth
                || tempBitmap.getHeight() > desiredHeight)) {
            bitmap = Bitmap.createScaledBitmap(tempBitmap,
                    desiredWidth, desiredHeight, true);
            tempBitmap.recycle();
        } else {
            bitmap = tempBitmap;
        }

        return bitmap;
    }

    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName  The complete path name for the file to be decoded.
     * @param maxWidth  Desired width of the bitmap.
     * @param maxHeight Desired height of the bitmap.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     * @see #createFromFile(File, int, int)
     */
    public static Bitmap createFromFile(String pathName, int maxWidth, int maxHeight) {
        return createFromFile(pathName, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), ImageView.ScaleType.CENTER_INSIDE);
    }

    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName  The complete path name for the file to be decoded.
     * @param maxWidth  Desired width of the bitmap.
     * @param maxHeight Desired height of the bitmap.
     * @param scaleType The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     * @see #createFromFile(File, int, int)
     */
    public static Bitmap createFromFile(String pathName, int maxWidth, int maxHeight,
                                        ImageView.ScaleType scaleType) {
        return createFromFile(pathName, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), scaleType);
    }

    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName      The complete path name for the file to be decoded.
     * @param maxWidth      Desired width of the bitmap.
     * @param maxHeight     Desired height of the bitmap.
     * @param decodeOptions The Bitmap decode config to be used.
     * @param scaleType     The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     * @see #createFromFile(File, int, int)
     */
    public static Bitmap createFromFile(String pathName,
                                        int maxWidth, int maxHeight,
                                        BitmapFactory.Options decodeOptions,
                                        ImageView.ScaleType scaleType) {
        File file = parse(pathName);

        return createFromFile(file, maxWidth, maxHeight, decodeOptions, scaleType);
    }

    /**
     * Decode a {@link File} file into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param file      The file to be decoded.
     * @param maxWidth  Desired width of the bitmap.
     * @param maxHeight Desired height of the bitmap.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap createFromFile(File file, int maxWidth, int maxHeight) {
        return createFromFile(file, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), ImageView.ScaleType.CENTER_INSIDE);
    }

    /**
     * Decode a {@link File} file into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param file      The file to be decoded.
     * @param maxWidth  Desired width of the bitmap.
     * @param maxHeight Desired height of the bitmap.
     * @param scaleType The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap createFromFile(File file, int maxWidth, int maxHeight,
                                        ImageView.ScaleType scaleType) {
        return createFromFile(file, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), scaleType);
    }

    /**
     * Decode a {@link File} file into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param file          The file to be decoded.
     * @param maxWidth      Desired width of the bitmap.
     * @param maxHeight     Desired height of the bitmap.
     * @param decodeOptions The Bitmap decode config to be used.
     * @param scaleType     The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap createFromFile(File file,
                                        int maxWidth, int maxHeight,
                                        BitmapFactory.Options decodeOptions,
                                        ImageView.ScaleType scaleType) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        if (maxWidth == 0 && maxHeight == 0) {
            return BitmapFactory.decodeFile(file.getAbsolutePath(), decodeOptions);
        }

        Bitmap bitmap;
        // If we have to resize this image, first get the natural bounds.
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), decodeOptions);
        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;

        // Then compute the dimensions we would ideally like to decode to.
        int desiredWidth = getResizedDimension(maxWidth, maxHeight,
                actualWidth, actualHeight, scaleType);
        int desiredHeight = getResizedDimension(maxHeight, maxWidth,
                actualHeight, actualWidth, scaleType);

        // Decode to the nearest power of two scaling factor.
        decodeOptions.inJustDecodeBounds = false;
        decodeOptions.inSampleSize =
                findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
        Bitmap tempBitmap =
                BitmapFactory.decodeFile(file.getAbsolutePath(), decodeOptions);

        // If necessary, scale down to the maximal acceptable size.
        if (tempBitmap != null
                && (tempBitmap.getWidth() > desiredWidth
                || tempBitmap.getHeight() > desiredHeight)) {
            bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
            tempBitmap.recycle();
        } else {
            bitmap = tempBitmap;
        }

        return bitmap;
    }

    /**
     * Decode a new Bitmap from a resource.
     *
     * @param resources  The resources object containing the image data.
     * @param resourceId The resource id of the image data.
     * @param maxWidth   Desired width of the bitmap.
     * @param maxHeight  Desired height of the bitmap.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap createFromResource(Resources resources, int resourceId,
                                            int maxWidth, int maxHeight) {
        return createFromResource(resources, resourceId, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), ImageView.ScaleType.CENTER_INSIDE);
    }

    /**
     * Decode a new Bitmap from a resource.
     *
     * @param resources  The resources object containing the image data.
     * @param resourceId The resource id of the image data.
     * @param maxWidth   Desired width of the bitmap.
     * @param maxHeight  Desired height of the bitmap.ì
     * @param scaleType  The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap createFromResource(Resources resources, int resourceId,
                                            int maxWidth, int maxHeight,
                                            ImageView.ScaleType scaleType) {
        return createFromResource(resources, resourceId, maxWidth, maxHeight,
                makeBitmapOptions(Bitmap.Config.RGB_565), scaleType);
    }

    /**
     * Decode a new Bitmap from a resource.
     *
     * @param resources     The resources object containing the image data.
     * @param resourceId    The resource id of the image data.
     * @param maxWidth      Desired width of the bitmap.
     * @param maxHeight     Desired height of the bitmap.
     * @param decodeOptions The Bitmap decode config to be used.
     * @param scaleType     The ScaleType used to calculate the needed image size.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap createFromResource(Resources resources, int resourceId,
                                            int maxWidth, int maxHeight,
                                            BitmapFactory.Options decodeOptions,
                                            ImageView.ScaleType scaleType) {

        if (maxWidth == 0 && maxHeight == 0) {
            return BitmapFactory.decodeResource(resources, resourceId, decodeOptions);
        }

        Bitmap bitmap;
        // If we have to resize this image, first get the natural bounds.
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resourceId, decodeOptions);
        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;

        // Then compute the dimensions we would ideally like to decode to.
        int desiredWidth = getResizedDimension(maxWidth, maxHeight,
                actualWidth, actualHeight, scaleType);
        int desiredHeight = getResizedDimension(maxHeight, maxWidth,
                actualHeight, actualWidth, scaleType);

        // Decode to the nearest power of two scaling factor.
        decodeOptions.inJustDecodeBounds = false;
        decodeOptions.inSampleSize =
                findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
        Bitmap tempBitmap = BitmapFactory.decodeResource(resources, resourceId, decodeOptions);

        // If necessary, scale down to the maximal acceptable size.
        if (tempBitmap != null
                && (tempBitmap.getWidth() > desiredWidth
                || tempBitmap.getHeight() > desiredHeight)) {
            bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
            tempBitmap.recycle();
        } else {
            bitmap = tempBitmap;
        }

        return bitmap;
    }

    /**
     * Creates a {@link File} file using the specified path.
     *
     * @param path The path to be used for the file.
     * @return The file for the specified path.
     */
    public static File parse(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        if (path.startsWith("file://")) {
            path = path.substring(7, path.length());
        }

        return new File(path);
    }

    /**
     * Creates the Bitmap decode options based on SDK version.
     *
     * @param decodeConfig The Bitmap decode config to be used.
     * @return the Bitmap options
     */
    @SuppressWarnings("deprecation")
    public static BitmapFactory.Options makeBitmapOptions(Bitmap.Config decodeConfig) {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        if (Build.VERSION.SDK_INT <= 19) {
            decodeOptions.inInputShareable = true;
            decodeOptions.inPurgeable = true;
        }
        decodeOptions.inPreferredConfig = decodeConfig;
        return decodeOptions;
    }
}
