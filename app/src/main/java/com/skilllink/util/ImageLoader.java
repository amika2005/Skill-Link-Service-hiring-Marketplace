package com.skilllink.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.io.IOException;
import java.io.InputStream;

public final class ImageLoader {

    private ImageLoader() {
    }

    public static void loadUriInto(@NonNull Context context,
                                   @NonNull ImageView target,
                                   @Nullable String uriString,
                                   @DrawableRes int placeholderRes,
                                   int placeholderTintRes) {
        if (TextUtils.isEmpty(uriString)) {
            applyPlaceholder(context, target, placeholderRes, placeholderTintRes);
            return;
        }

        try {
            Uri uri = Uri.parse(uriString);
            ContentResolver resolver = context.getContentResolver();
            Bitmap bitmap;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, srcRect) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    decoder.setOnPartialImageListener(exception -> true);
                });
            } else {
                try (InputStream inputStream = resolver.openInputStream(uri)) {
                    if (inputStream == null) {
                        applyPlaceholder(context, target, placeholderRes, placeholderTintRes);
                        return;
                    }
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
            }

            if (bitmap != null) {
                ImageViewCompat.setImageTintList(target, null);
                target.clearColorFilter();
                target.setImageBitmap(bitmap);
            } else {
                applyPlaceholder(context, target, placeholderRes, placeholderTintRes);
            }
        } catch (IOException | SecurityException exception) {
            applyPlaceholder(context, target, placeholderRes, placeholderTintRes);
        }
    }

    private static void applyPlaceholder(@NonNull Context context,
                                         @NonNull ImageView target,
                                         @DrawableRes int placeholderRes,
                                         int placeholderTintRes) {
        target.setImageResource(placeholderRes);
        if (placeholderTintRes != 0) {
            int tint = ContextCompat.getColor(context, placeholderTintRes);
            ImageViewCompat.setImageTintList(target, ColorStateList.valueOf(tint));
        } else {
            ImageViewCompat.setImageTintList(target, null);
            target.clearColorFilter();
        }
    }
}
