package com.foellerich.mymapmarkers;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.foellerich.mymapmarkers.net.MarkerParser;
import com.foellerich.mymapmarkers.provider.MarkerContract;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by foellerich on 27.04.2015.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "MarkerSyncAdapter";
    private static final String MARKER_URL = "https://mymapmarkers.herokuapp.com/markers";
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000; // 15 seconds
    private static final int NET_READ_TIMEOUT_MILLIS = 10000; // 10 seconds

    private final ContentResolver mContentResolver;

    private static final String[] PROJECTION = new String[] {
            MarkerContract.Marker._ID,
            MarkerContract.Marker.COLUMN_NAME_ENTRY_ID,
            MarkerContract.Marker.COLUMN_NAME_NAME,
            MarkerContract.Marker.COLUMN_NAME_ADDRESS,
            MarkerContract.Marker.COLUMN_NAME_LAT,
            MarkerContract.Marker.COLUMN_NAME_LNG
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_ENTRY_ID = 1;
    public static final int COLUMN_NAME = 2;
    public static final int COLUMN_ADDRESS = 3;
    public static final int COLUMN_LAT = 4;
    public static final int COLUMN_LNG = 5;

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Beginning sync");
        try {
            final URL location = new URL(MARKER_URL);
            InputStream stream = null;

            try {
                Log.i(TAG, "streaming data from Network: " + location);
                stream = downloadUrl(location);
                updateLocalData(stream, syncResult);

            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        } catch (RemoteException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        }
    }

    private void updateLocalData(final InputStream stream, final SyncResult syncResult)
            throws IOException, RemoteException, OperationApplicationException {
        final MarkerParser markerParser = new MarkerParser();
        final ContentResolver contentResolver = getContext().getContentResolver();

        Log.i(TAG, "parsing stream as JSON");
        final List<MarkerParser.Entry> entries = markerParser.parse(stream);
        Log.i(TAG, "Parsing complete. Found " + entries.size() + " entries");

        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        HashMap<String, MarkerParser.Entry> entryMap = new HashMap<String, MarkerParser.Entry>();
        for (MarkerParser.Entry e : entries) {
            entryMap.put(e.id, e);
        }

        Log.i(TAG, "Fetching local entries for merge");
        Uri uri = MarkerContract.Marker.CONTENT_URI;
        Cursor c = contentResolver.query(uri, PROJECTION, null, null, null);
        assert c != null;
        Log.i(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");

        int id;
        String entryId;
        String name;
        String address;
        double lat;
        double lng;
        while(c.moveToNext()) {
            syncResult.stats.numEntries++;
            id = c.getInt(COLUMN_ID);
            entryId = c.getString(COLUMN_ENTRY_ID);
            name = c.getString(COLUMN_NAME);
            address = c.getString(COLUMN_ADDRESS);
            lat = c.getDouble(COLUMN_LAT);
            lng = c.getDouble(COLUMN_LNG);
            MarkerParser.Entry match = entryMap.get(entryId);
            if (match != null) {
                entryMap.remove(entryId);
                Uri existingUri = MarkerContract.Marker.CONTENT_URI.buildUpon()
                        .appendPath(Integer.toString(id)).build();
                if ((match.name != null && !match.name.equals(name)) ||
                        (match.address != null && !match.address.equals(address)) ||
                        (match.lat != lat) ||
                        (match.lng != lng)) {
                    Log.i(TAG, "Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(MarkerContract.Marker.COLUMN_NAME_NAME, match.name)
                            .withValue(MarkerContract.Marker.COLUMN_NAME_ADDRESS, match.address)
                            .withValue(MarkerContract.Marker.COLUMN_NAME_LAT, match.lat)
                            .withValue(MarkerContract.Marker.COLUMN_NAME_LNG, match.lng)
                            .build());
                    syncResult.stats.numUpdates++;
                } else {
                    Log.i(TAG, "No action: " + existingUri);
                }
            } else {
                Uri deleteUri = MarkerContract.Marker.CONTENT_URI.buildUpon()
                        .appendPath(Integer.toString(id)).build();
                Log.i(TAG, "Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();

        for (MarkerParser.Entry e : entryMap.values()) {
            Log.i(TAG, "Scheduling insert: entry_id=" + e.id);
            batch.add(ContentProviderOperation.newInsert(MarkerContract.Marker.CONTENT_URI)
                    .withValue(MarkerContract.Marker.COLUMN_NAME_NAME, e.name)
                    .withValue(MarkerContract.Marker.COLUMN_NAME_ADDRESS, e.address)
                    .withValue(MarkerContract.Marker.COLUMN_NAME_LAT, e.lat)
                    .withValue(MarkerContract.Marker.COLUMN_NAME_LNG, e.lng)
                    .build());
            syncResult.stats.numInserts++;
        }
        Log.i(TAG, "Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(MarkerContract.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(
                MarkerContract.Marker.CONTENT_URI,
                null,
                false
        );
    }

    private InputStream downloadUrl(final URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS);
        conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        return conn.getInputStream();
    }
}
