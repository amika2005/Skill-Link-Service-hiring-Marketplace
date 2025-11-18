package com.skilllink.util;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skilllink.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ServiceCategoryRegistry {

    private static final List<Category> CATEGORIES;
    private static final Map<String, Category> CATEGORY_BY_KEY;
    private static final Map<String, Category> CATEGORY_BY_NAME;

    static {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("electrician", "Electrician", 18, R.drawable.electrician));
        categories.add(new Category("plumber", "Plumber", 20, R.drawable.plumber));
        categories.add(new Category("carpenter", "Carpenter", 15, R.drawable.carpenter));
        categories.add(new Category("mechanic", "Mechanic", 24, R.drawable.mechanic));
        categories.add(new Category("cleaning", "Cleaning", 22, R.drawable.cleaning));
        categories.add(new Category("gardener", "Gardener", 12, R.drawable.gardener));
        categories.add(new Category("painting", "Painter", 16, R.drawable.painting));
        categories.add(new Category("it_support", "IT Support", 14, R.drawable.mechanic));

        CATEGORIES = Collections.unmodifiableList(categories);

        Map<String, Category> byKey = new HashMap<>();
        Map<String, Category> byName = new HashMap<>();
        for (Category category : categories) {
            byKey.put(category.getKey(), category);
            byName.put(category.getDisplayName().toLowerCase(Locale.getDefault()), category);
        }
        CATEGORY_BY_KEY = Collections.unmodifiableMap(byKey);
        CATEGORY_BY_NAME = Collections.unmodifiableMap(byName);
    }

    private ServiceCategoryRegistry() {
        // Utility class
    }

    @NonNull
    public static List<Category> getCategories() {
        return CATEGORIES;
    }

    @Nullable
    public static Category findByKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        return CATEGORY_BY_KEY.get(key.toLowerCase(Locale.getDefault()));
    }

    @Nullable
    public static Category findByDisplayName(@Nullable String displayName) {
        if (displayName == null) {
            return null;
        }
        return CATEGORY_BY_NAME.get(displayName.toLowerCase(Locale.getDefault()));
    }

    @Nullable
    public static Category resolve(@Nullable String value) {
        Category byKey = findByKey(value);
        if (byKey != null) {
            return byKey;
        }
        return findByDisplayName(value);
    }

    @Nullable
    public static String resolveKey(@Nullable String value) {
        Category resolved = resolve(value);
        return resolved != null ? resolved.getKey() : null;
    }

    @NonNull
    public static String getDisplayNameOrDefault(@Nullable String value) {
        Category resolved = resolve(value);
        if (resolved != null) {
            return resolved.getDisplayName();
        }
        return value != null ? value : "";
    }

    @NonNull
    public static String[] getDisplayNames() {
        String[] names = new String[CATEGORIES.size()];
        for (int i = 0; i < CATEGORIES.size(); i++) {
            names[i] = CATEGORIES.get(i).getDisplayName();
        }
        return names;
    }

    public static final class Category {
        private final String key;
        private final String displayName;
        private final int defaultWorkersCount;
        @DrawableRes
        private final int iconResId;

        private Category(@NonNull String key,
                         @NonNull String displayName,
                         int defaultWorkersCount,
                         @DrawableRes int iconResId) {
            this.key = key;
            this.displayName = displayName;
            this.defaultWorkersCount = defaultWorkersCount;
            this.iconResId = iconResId;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }

        public int getDefaultWorkersCount() {
            return defaultWorkersCount;
        }

        @DrawableRes
        public int getIconResId() {
            return iconResId;
        }
    }
}
