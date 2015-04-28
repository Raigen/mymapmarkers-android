package com.foellerich.mymapmarkers;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by foellerich on 26.04.2015.
 */
public class SyncService extends Service {
    private static final String TAG = "MarkerSyncService";

    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true, true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
