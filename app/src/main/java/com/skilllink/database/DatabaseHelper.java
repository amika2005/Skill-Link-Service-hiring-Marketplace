package com.skilllink.database;

import static android.app.DownloadManager.COLUMN_DESCRIPTION;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "skill_link.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_WORKERS = "workers";
    public static final String TABLE_SERVICES = "services";
    public static final String TABLE_BOOKINGS = "bookings";
    public static final String TABLE_REVIEWS = "reviews";
    public static final String TABLE_PAYMENTS = "payments";
    public static final String TABLE_CHAT_MESSAGES = "chat_messages";
    public static final String TABLE_SERVICE_CATEGORIES = "service_categories";
    public static final String TABLE_WORKER_CATEGORIES = "worker_categories";
    public static final String TABLE_ADDRESSES = "addresses";
    public static final String TABLE_FAVORITE_WORKERS = "favorite_workers";

    // Common columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";

    // Users table columns
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_PHONE_NUMBER = "phone_number";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_FULL_NAME = "full_name";
    public static final String COLUMN_AVATAR_URL = "avatar_url";
    public static final String COLUMN_USER_TYPE = "user_type"; // customer/worker
    public static final String COLUMN_VERIFICATION_STATUS = "verification_status";
    public static final String COLUMN_METADATA = "metadata";

    // Workers table columns
    public static final String COLUMN_WORKER_ID = "worker_id";
    public static final String COLUMN_SERVICE_CATEGORIES = "service_categories";
    public static final String COLUMN_EXPERIENCE_YEARS = "experience_years";
    public static final String COLUMN_RATING_AVERAGE = "rating_average";
    public static final String COLUMN_TOTAL_JOBS = "total_jobs";
    public static final String COLUMN_VERIFICATION_DOCUMENTS = "verification_documents";
    public static final String COLUMN_AVAILABILITY_STATUS = "availability_status";
    public static final String COLUMN_LOCATION_LAT = "location_lat";
    public static final String COLUMN_LOCATION_LNG = "location_lng";
    public static final String COLUMN_SERVICE_AREA_RADIUS = "service_area_radius";

    // Services table columns
    public static final String COLUMN_SERVICE_ID = "service_id";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_SUB_CATEGORY = "sub_category";
    public static final String COLUMN_SERVICE_NAME = "service_name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_BASE_PRICE = "base_price";
    public static final String COLUMN_PRICE_TYPE = "price_type"; // fixed/hourly
    public static final String COLUMN_AVERAGE_DURATION = "average_duration";
    public static final String COLUMN_ICON_URL = "icon_url";
    public static final String COLUMN_BOOKING_ID = "booking_id";
    public static final String COLUMN_CUSTOMER_ID = "customer_id";
    public static final String COLUMN_BOOKING_WORKER_ID = "worker_id";
    public static final String COLUMN_BOOKING_SERVICE_ID = "service_id";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_SCHEDULED_TIME = "scheduled_time";
    public static final String COLUMN_LOCATION_LAT_BOOKING = "location_lat";
    public static final String COLUMN_LOCATION_LNG_BOOKING = "location_lng";
    public static final String COLUMN_TOTAL_AMOUNT = "total_amount";
    public static final String COLUMN_PAYMENT_STATUS = "payment_status";
    public static final String COLUMN_TRACKING_DATA = "tracking_data";

    // Reviews table columns
    public static final String COLUMN_REVIEW_ID = "review_id";
    public static final String COLUMN_REVIEW_BOOKING_ID = "booking_id";
    public static final String COLUMN_RATING = "rating";
    public static final String COLUMN_COMMENT = "comment";
    public static final String COLUMN_PHOTOS = "photos";
    public static final String COLUMN_RESPONSE = "response";

    // Payments table columns
    public static final String COLUMN_PAYMENT_ID = "payment_id";
    public static final String COLUMN_PAYMENT_BOOKING_ID = "booking_id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_METHOD = "method";
    public static final String COLUMN_TRANSACTION_ID = "transaction_id";
    public static final String COLUMN_PAYMENT_STATUS_PAYMENT = "status";
    public static final String COLUMN_PAYMENT_METADATA = "metadata";

    // Chat messages table columns
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_MESSAGE_BOOKING_ID = "booking_id";
    public static final String COLUMN_SENDER_ID = "sender_id";
    public static final String COLUMN_MESSAGE_TYPE = "message_type";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_READ_STATUS = "read_status";
    public static final String COLUMN_MESSAGE_CREATED_AT = "created_at";

    // Service categories table columns
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_CATEGORY_NAME = "category_name";
    public static final String COLUMN_CATEGORY_ICON = "category_icon";
    public static final String COLUMN_IS_ACTIVE = "is_active";

    // Worker categories table columns (many-to-many relationship)
    public static final String COLUMN_WORKER_CATEGORY_ID = "worker_category_id";
    public static final String COLUMN_WORKER_REF_ID = "worker_id";
    public static final String COLUMN_CATEGORY_REF_ID = "category_id";

    // Addresses table columns
    public static final String COLUMN_ADDRESS_ID = "address_id";
    public static final String COLUMN_USER_REF_ID = "user_id";
    public static final String COLUMN_ADDRESS_TYPE = "address_type"; // home, work, other
    public static final String COLUMN_STREET = "street";
    public static final String COLUMN_CITY = "city";
    public static final String COLUMN_POSTAL_CODE = "postal_code";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_IS_DEFAULT = "is_default";

    // Favorite workers table columns
    public static final String COLUMN_FAVORITE_ID = "favorite_id";
    public static final String COLUMN_USER_REF_ID_FAVORITE = "user_id";
    public static final String COLUMN_WORKER_REF_ID_FAVORITE = "worker_id";

    // Create tables SQL statements
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " (" +
            COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PHONE_NUMBER + " TEXT UNIQUE, " +
            COLUMN_EMAIL + " TEXT UNIQUE, " +
            COLUMN_FULL_NAME + " TEXT, " +
            COLUMN_AVATAR_URL + " TEXT, " +
            COLUMN_USER_TYPE + " TEXT, " +
            COLUMN_VERIFICATION_STATUS + " TEXT, " +
            COLUMN_METADATA + " TEXT, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")";

    private static final String CREATE_TABLE_WORKERS = "CREATE TABLE " + TABLE_WORKERS + " (" +
            COLUMN_WORKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_ID + " INTEGER, " +
            COLUMN_EXPERIENCE_YEARS + " INTEGER, " +
            COLUMN_RATING_AVERAGE + " REAL DEFAULT 0.0, " +
            COLUMN_TOTAL_JOBS + " INTEGER DEFAULT 0, " +
            COLUMN_VERIFICATION_DOCUMENTS + " TEXT, " +
            COLUMN_AVAILABILITY_STATUS + " TEXT, " +
            COLUMN_LOCATION_LAT + " REAL, " +
            COLUMN_LOCATION_LNG + " REAL, " +
            COLUMN_SERVICE_AREA_RADIUS + " REAL, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")" +
            ")";

    private static final String CREATE_TABLE_SERVICES = "CREATE TABLE " + TABLE_SERVICES + " (" +
            COLUMN_SERVICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CATEGORY + " TEXT, " +
            COLUMN_SUB_CATEGORY + " TEXT, " +
            COLUMN_SERVICE_NAME + " TEXT, " +
            COLUMN_DESCRIPTION + " TEXT, " +
            COLUMN_BASE_PRICE + " REAL, " +
            COLUMN_PRICE_TYPE + " TEXT, " +
            COLUMN_AVERAGE_DURATION + " INTEGER, " +
            COLUMN_ICON_URL + " TEXT, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")";

    private static final String CREATE_TABLE_BOOKINGS = "CREATE TABLE " + TABLE_BOOKINGS + " (" +
            COLUMN_BOOKING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CUSTOMER_ID + " INTEGER, " +
            COLUMN_WORKER_ID + " INTEGER, " +
            COLUMN_SERVICE_ID + " INTEGER, " +
            COLUMN_STATUS + " TEXT, " +
            COLUMN_SCHEDULED_TIME + " DATETIME, " +
            COLUMN_LOCATION_LAT_BOOKING + " REAL, " +
            COLUMN_LOCATION_LNG_BOOKING + " REAL, " +
            COLUMN_TOTAL_AMOUNT + " REAL, " +
            COLUMN_PAYMENT_STATUS + " TEXT, " +
            COLUMN_TRACKING_DATA + " TEXT, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_CUSTOMER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "), " +
            "FOREIGN KEY(" + COLUMN_WORKER_ID + ") REFERENCES " + TABLE_WORKERS + "(" + COLUMN_WORKER_ID + "), " +
            "FOREIGN KEY(" + COLUMN_SERVICE_ID + ") REFERENCES " + TABLE_SERVICES + "(" + COLUMN_SERVICE_ID + ")" +
            ")";

    private static final String CREATE_TABLE_REVIEWS = "CREATE TABLE " + TABLE_REVIEWS + " (" +
            COLUMN_REVIEW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_BOOKING_ID + " INTEGER, " +
            COLUMN_RATING + " INTEGER, " +
            COLUMN_COMMENT + " TEXT, " +
            COLUMN_PHOTOS + " TEXT, " +
            COLUMN_RESPONSE + " TEXT, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_BOOKING_ID + ") REFERENCES " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_ID + ")" +
            ")";

    private static final String CREATE_TABLE_PAYMENTS = "CREATE TABLE " + TABLE_PAYMENTS + " (" +
            COLUMN_PAYMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PAYMENT_BOOKING_ID + " INTEGER, " +
            COLUMN_AMOUNT + " REAL, " +
            COLUMN_METHOD + " TEXT, " +
            COLUMN_TRANSACTION_ID + " TEXT, " +
            COLUMN_PAYMENT_STATUS + " TEXT, " +
            COLUMN_PAYMENT_METADATA + " TEXT, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_PAYMENT_BOOKING_ID + ") REFERENCES " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_ID + ")" +
            ")";

    private static final String CREATE_TABLE_CHAT_MESSAGES = "CREATE TABLE " + TABLE_CHAT_MESSAGES + " (" +
            COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_MESSAGE_BOOKING_ID + " INTEGER, " +
            COLUMN_SENDER_ID + " INTEGER, " +
            COLUMN_MESSAGE_TYPE + " TEXT, " +
            COLUMN_CONTENT + " TEXT, " +
            COLUMN_READ_STATUS + " TEXT, " +
            COLUMN_MESSAGE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_MESSAGE_BOOKING_ID + ") REFERENCES " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_ID + ")" +
            ")";

    private static final String CREATE_TABLE_SERVICE_CATEGORIES = "CREATE TABLE " + TABLE_SERVICE_CATEGORIES + " (" +
            COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CATEGORY_NAME + " TEXT, " +
            COLUMN_CATEGORY_ICON + " TEXT, " +
            COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")";

    private static final String CREATE_TABLE_WORKER_CATEGORIES = "CREATE TABLE " + TABLE_WORKER_CATEGORIES + " (" +
            COLUMN_WORKER_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_WORKER_REF_ID + " INTEGER, " +
            COLUMN_CATEGORY_REF_ID + " INTEGER, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_WORKER_REF_ID + ") REFERENCES " + TABLE_WORKERS + "(" + COLUMN_WORKER_ID + "), " +
            "FOREIGN KEY(" + COLUMN_CATEGORY_REF_ID + ") REFERENCES " + TABLE_SERVICE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ")" +
            ")";

    private static final String CREATE_TABLE_ADDRESSES = "CREATE TABLE " + TABLE_ADDRESSES + " (" +
            COLUMN_ADDRESS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_REF_ID + " INTEGER, " +
            COLUMN_ADDRESS_TYPE + " TEXT, " +
            COLUMN_STREET + " TEXT, " +
            COLUMN_CITY + " TEXT, " +
            COLUMN_POSTAL_CODE + " TEXT, " +
            COLUMN_LATITUDE + " REAL, " +
            COLUMN_LONGITUDE + " REAL, " +
            COLUMN_IS_DEFAULT + " INTEGER DEFAULT 0, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_USER_REF_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")" +
            ")";

    private static final String CREATE_TABLE_FAVORITE_WORKERS = "CREATE TABLE " + TABLE_FAVORITE_WORKERS + " (" +
            COLUMN_FAVORITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_REF_ID_FAVORITE + " INTEGER, " +
            COLUMN_WORKER_REF_ID_FAVORITE + " INTEGER, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_USER_REF_ID_FAVORITE + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "), " +
            "FOREIGN KEY(" + COLUMN_WORKER_REF_ID_FAVORITE + ") REFERENCES " + TABLE_WORKERS + "(" + COLUMN_WORKER_ID + ")" +
            ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all tables
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_WORKERS);
        db.execSQL(CREATE_TABLE_SERVICES);
        db.execSQL(CREATE_TABLE_BOOKINGS);
        db.execSQL(CREATE_TABLE_REVIEWS);
        db.execSQL(CREATE_TABLE_PAYMENTS);
        db.execSQL(CREATE_TABLE_CHAT_MESSAGES);
        db.execSQL(CREATE_TABLE_SERVICE_CATEGORIES);
        db.execSQL(CREATE_TABLE_WORKER_CATEGORIES);
        db.execSQL(CREATE_TABLE_ADDRESSES);
        db.execSQL(CREATE_TABLE_FAVORITE_WORKERS);

        // Insert default service categories
        insertDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE_WORKERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADDRESSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKER_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REVIEWS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        // Create tables again
        onCreate(db);
    }

    private void insertDefaultCategories(SQLiteDatabase db) {
        // Insert default service categories
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Mechanics', 'mechanic_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Plumbers', 'plumber_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Electricians', 'electrician_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Carpenters', 'carpenter_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Painters', 'painter_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Cleaners', 'cleaner_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Movers', 'mover_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Technicians', 'technician_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Welders', 'welder_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Masons', 'mason_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Gardeners', 'gardener_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Pest Control', 'pest_control_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('Solar Panel Services', 'solar_icon')");
        db.execSQL("INSERT INTO " + TABLE_SERVICE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_ICON + ") VALUES ('CCTV Installation', 'cctv_icon')");
    }
}