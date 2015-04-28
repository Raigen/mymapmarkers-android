package com.foellerich.mymapmarkers.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by foellerich on 26.04.2015.
 */
public class MarkerContract {
    private MarkerContract() {}

    public static final String CONTENT_AUTHORITY = "com.foellerich.mymapmarkers";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_ENTRIES = "markers";

    public static class Marker implements BaseColumns {
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.mymapmarkers.markers";
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.mymapmarkers.marker";
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ENTRIES).build();
        public static final String TABLE_NAME = "marker";
        public static final String COLUMN_NAME_ENTRY_ID = "marker_id";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LNG = "lng";

    }
}
