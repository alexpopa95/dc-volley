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

package com.android.volley;

/**
 * Encapsulates a parsed response for delivery.
 *
 * @param <T> Parsed type of this response
 */
public class Response<T> {

    /**
     * Callback interface for delivering parsed responses.
     */
    public interface Listener<T> {
        /**
         * Called when a response is received.
         *
         * @param response The response of the given type
         */
        public void onResponse(T response);
    }

    /**
     * Callback interface for delivering error responses.
     */
    public interface ErrorListener {
        /**
         * Callback method that an error has been occurred with the
         * provided error code and optional user-readable message.
         *
         * @param error The response error
         */
        public void onErrorResponse(VolleyError error);
    }

    /**
     * Callback interface for delivering the progress of the responses.
     */
    public interface ProgressListener {

        /**
         * Callback called each time byte chunks are downloaded
         *
         * @param progress         Progress percentage from 0 to 100
         * @param transferredBytes Downloaded bytes
         * @param totalSize        Total response size
         * @param millisSpent      Current time spent on download
         * @param retryCount       Current request retries
         */
        public void onProgress(int progress, long transferredBytes, long totalSize, long millisSpent, int retryCount);

    }

    /**
     * Returns a successful response containing the parsed result.
     *
     * @param result     A result of the expected type
     * @param cacheEntry A cache entry for this response
     * @param <T>        Parsed type of this response
     * @return A successful response
     */
    public static <T> Response<T> success(T result, Cache.Entry cacheEntry) {
        return new Response<T>(result, cacheEntry);
    }

    /**
     * Returns a failed response containing the given error code and an optional
     * localized message displayed to the user.
     *
     * @param error A response error
     * @param <T>   Parsed type of this response
     * @return A failed response
     */
    public static <T> Response<T> error(VolleyError error) {
        return new Response<T>(error);
    }

    /**
     * Parsed response, or null in the case of error.
     */
    public final T result;

    /**
     * Cache metadata for this response, or null in the case of error.
     */
    public final Cache.Entry cacheEntry;

    /**
     * Detailed error information if <code>errorCode != OK</code>.
     */
    public final VolleyError error;

    /**
     * True if this response was a soft-expired one and a second one MAY be coming.
     */
    public boolean intermediate = false;

    /**
     * Returns whether this response is considered successful.
     *
     * @return <code>true</code> if the response is successful;
     * <code>false</code> otherwise
     */
    public boolean isSuccess() {
        return error == null;
    }


    private Response(T result, Cache.Entry cacheEntry) {
        this.result = result;
        this.cacheEntry = cacheEntry;
        this.error = null;
    }

    private Response(VolleyError error) {
        this.result = null;
        this.cacheEntry = null;
        this.error = error;
    }
}
