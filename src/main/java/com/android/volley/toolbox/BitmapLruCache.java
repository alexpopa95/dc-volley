package com.android.volley.toolbox;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.LruCache;

/**
 * Created by andrea on 07/06/17.
 */

public class BitmapLruCache implements ImageLoader.ImageCache {

    private LruCache<String, Bitmap> mLruCache;

    public BitmapLruCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    return bitmap.getByteCount() / 1024;
                }
                return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
            }
        };
    }

    @Override
    public Bitmap getBitmap(String url) {
        if (mLruCache == null) {
            return null;
        }
        return mLruCache.get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        if (mLruCache == null) {
            return;
        }
        mLruCache.put(url, bitmap);
    }

}
