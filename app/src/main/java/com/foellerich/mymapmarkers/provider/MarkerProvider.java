package com.foellerich.mymapmarkers.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.example.android.common.db.SelectionBuilder;

/**
 * Created by foellerich on 26.04.2015.
 */
public class MarkerProvider extends ContentProvider {
    MarkerDatabase mDatabaseHelper;

    private static String AUTHORITY = MarkerContract.CONTENT_AUTHORITY;

    public static final int ROUTE_MARKERS = 1;
    public static final int ROUTE_MARKERS_ID = 2;

    public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "markers", ROUTE_MARKERS);
        sUriMatcher.addURI(AUTHORITY, "markers/*", ROUTE_MARKERS_ID);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new MarkerDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ROUTE_MARKERS_ID:
                String id = uri.getLastPathSegment();
                builder.where(MarkerContract.Marker._ID + "=?", id);
            case ROUTE_MARKERS:
                builder.table(MarkerContract.Marker.TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor c = builder.query(db, projection, sortOrder);
                Context ctx = getContext();
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_MARKERS:
                return MarkerContract.Marker.CONTENT_TYPE;
            case ROUTE_MARKERS_ID:
                return MarkerContract.Marker.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        switch (match) {
            case ROUTE_MARKERS:
                long id = db.insertOrThrow(MarkerContract.Marker.TABLE_NAME, null, values);
                result = Uri.parse(MarkerContract.Marker.CONTENT_URI + "/" + id);
                break;
            case ROUTE_MARKERS_ID:
                throw new UnsupportedOperationException("Insert not supported on uri: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_MARKERS:
                count = builder.table(MarkerContract.Marker.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case ROUTE_MARKERS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(MarkerContract.Marker.TABLE_NAME)
                        .where(MarkerContract.Marker._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_MARKERS:
                count = builder.table(MarkerContract.Marker.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ROUTE_MARKERS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(MarkerContract.Marker.TABLE_NAME)
                        .where(MarkerContract.Marker._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    static class MarkerDatabase extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "mymapmarkers.db";
        private static final String TYPE_TEXT = " TEXT";
        private static final String TYPE_COORDS = " VARCHAR(20)";
        private static final String COMMA_SEP = ",";
        private static final String SQL_CREATE_MARKERS =
                "CREATE TABLE " + MarkerContract.Marker.TABLE_NAME + " (" +
                        MarkerContract.Marker._ID + " INTEGER PRIMARY KEY," +
                        MarkerContract.Marker.COLUMN_NAME_ENTRY_ID + TYPE_TEXT + COMMA_SEP +
                        MarkerContract.Marker.COLUMN_NAME_NAME + TYPE_TEXT + COMMA_SEP +
                        MarkerContract.Marker.COLUMN_NAME_ADDRESS + TYPE_TEXT + COMMA_SEP +
                        MarkerContract.Marker.COLUMN_NAME_LAT + TYPE_COORDS + COMMA_SEP +
                        MarkerContract.Marker.COLUMN_NAME_LNG + TYPE_COORDS + ")";

        private static final String SQL_DELETE_MARKERS =
                "DROP TABLE IF EXISTS " + MarkerContract.Marker.TABLE_NAME;
        public MarkerDatabase(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {db.execSQL(SQL_CREATE_MARKERS);}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // this database is only a cache for online data
            // so on upgrade just discard the data and start over
            db.execSQL(SQL_DELETE_MARKERS);
            onCreate(db);
        }
    }
}
