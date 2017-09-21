/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.toolbox;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.widget.ImageView.ScaleType;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A canned request for getting an image at a given URL and calling
 * back with a decoded Bitmap.
 */
public class ImageRequest extends Request<Bitmap> implements Response.ProgressListener {
    /**
     * Socket timeout in milliseconds for image requests
     */
    private static final int IMAGE_TIMEOUT_MS = 1000;

    /**
     * Default number of retries for image requests
     */
    private static final int IMAGE_MAX_RETRIES = 2;

    /**
     * Default backoff multiplier for image requests
     */
    private static final float IMAGE_BACKOFF_MULT = 2f;

    private final Response.Listener<Bitmap> mListener;
    private final Response.ProgressListener mProgressListener;
    private final Config mDecodeConfig;
    private final int mMaxWidth;
    private final int mMaxHeight;
    private ScaleType mScaleType;

    /**
     * Decoding lock so that we don't decode more than one image at a time (to avoid OOM's)
     */
    private static final Object sDecodeLock = new Object();

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url              URL of the image
     * @param listener         Listener to receive the decoded bitmap
     * @param maxWidth         Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight        Maximum height to decode this bitmap to, or zero for
     *                         none
     * @param scaleType        The ImageViews ScaleType used to calculate the needed image size.
     * @param decodeConfig     Format to decode the bitmap to
     * @param errorListener    Error listener, or null to ignore errors
     * @param progressListener The image progress listener
     */
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
                        ScaleType scaleType, Config decodeConfig, Response.ErrorListener errorListener,
                        Response.ProgressListener progressListener) {
        super(Method.GET, url, errorListener);
        setRetryPolicy(
                new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
        mListener = listener;
        mProgressListener = progressListener;
        mDecodeConfig = decodeConfig;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mScaleType = scaleType;
    }

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url              URL of the image
     * @param listener         Listener to receive the decoded bitmap
     * @param maxWidth         Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight        Maximum height to decode this bitmap to, or zero for
     *                         none
     * @param scaleType        The ImageViews ScaleType used to calculate the needed image size.
     * @param decodeConfig     Format to decode the bitmap to
     * @param errorListener    Error listener, or null to ignore errors
     */
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
                        ScaleType scaleType, Config decodeConfig, Response.ErrorListener errorListener) {
        this(url, listener, maxWidth, maxHeight, scaleType, decodeConfig, errorListener, null);
    }

    /**
     * For API compatibility with the pre-ScaleType variant of the constructor. Equivalent to
     * the normal constructor with {@code ScaleType.CENTER_INSIDE}.
     *
     * @param url              URL of the image
     * @param listener         Listener to receive the decoded bitmap
     * @param maxWidth         Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight        Maximum height to decode this bitmap to, or zero for
     *                         none
     * @param decodeConfig     Format to decode the bitmap to
     * @param errorListener    Error listener, or null to ignore errors
     * @param progressListener The image progress listener
     */
    @Deprecated
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
                        Config decodeConfig, Response.ErrorListener errorListener,
                        Response.ProgressListener progressListener) {
        this(url, listener, maxWidth, maxHeight, ScaleType.CENTER_INSIDE, decodeConfig, errorListener, progressListener);
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (sDecodeLock) {
            try {
                if (isFile(getUrl())) {
                    return doFileParse();
                }
                return doParse(response);
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     * <p>
     * This version is for reading a Bitmap from file
     */
    private Response<Bitmap> doFileParse() {
        File bitmapFile = ImageUtils.parse(getUrl());
        if (bitmapFile == null || !bitmapFile.exists() || !bitmapFile.isFile()) {
            return Response.error(new ParseError(new FileNotFoundException(
                    String.format("File not found: %s", bitmapFile.getAbsolutePath()))));
        }

        BitmapFactory.Options decodeOptions = ImageUtils.makeBitmapOptions(mDecodeConfig);
        Bitmap bitmap = ImageUtils.createFromFile(bitmapFile, mMaxWidth, mMaxHeight,
                decodeOptions, mScaleType);

        if (bitmap == null) {
            return Response.error(new ParseError());
        }

        return Response.success(bitmap, HttpHeaderParser.parseBitmapCacheHeaders(bitmap));
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    private Response<Bitmap> doParse(NetworkResponse response) {
        byte[] data = response.data;

        BitmapFactory.Options decodeOptions = ImageUtils.makeBitmapOptions(mDecodeConfig);
        Bitmap bitmap = ImageUtils.create(data, mMaxWidth, mMaxHeight, decodeOptions, mScaleType);

        if (bitmap == null) {
            return Response.error(new ParseError(response));
        }

        return Response.success(bitmap, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(Bitmap response) {
        mListener.onResponse(response);
    }

    @Override
    public void onProgress(int progress, long transferredBytes, long totalSize, long millisSpent, int retryCount) {
        if (mProgressListener != null) {
            mProgressListener.onProgress(progress, transferredBytes, totalSize, millisSpent, retryCount);
        }
    }
}
