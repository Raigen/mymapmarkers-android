package com.foellerich.mymapmarkers;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.example.android.common.accounts.GenericAccountService;
import com.foellerich.mymapmarkers.provider.MarkerContract;

/**
 * Created by foellerich on 28.04.2015.
 */
public class MainListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MainMarkerFragment";

    private SimpleCursorAdapter mAdapter;
    private Object mSyncObserverHandle;
    private Menu mOptionsMenu;
    private static final String[] PROJECTION = new String[]{
            MarkerContract.Marker._ID,
            MarkerContract.Marker.COLUMN_NAME_NAME,
            MarkerContract.Marker.COLUMN_NAME_ADDRESS,
            MarkerContract.Marker.COLUMN_NAME_LAT,
            MarkerContract.Marker.COLUMN_NAME_LNG
    };

    private static final int COLUMN_ID = 0;
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_ADDRESS = 2;
    private static final int COLUMN_LAT = 3;
    private static final int COLUMN_LNG = 4;

    private static final String[] FROM_COLUMNS = new String[]{
            MarkerContract.Marker.COLUMN_NAME_NAME,
            MarkerContract.Marker.COLUMN_NAME_ADDRESS
    };

    private static final int[] TO_FIELDS = new int[]{
            android.R.id.text1,
            android.R.id.text2
    };

    public MainListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        SyncUtils.CreateSyncAccount(activity);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new SimpleCursorAdapter(
                getActivity(),              // current context
                android.R.layout.simple_list_item_activated_2, // layout for rows
                null,                       // Cursor
                FROM_COLUMNS,               // columns to use
                TO_FIELDS,                  // fields to use
                0                           // no flags
        );
        setListAdapter(mAdapter);
        setEmptyText(getText(R.string.loading));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // we only have one loader so we can ignore `i`
        return new CursorLoader(getActivity(), // context
                MarkerContract.Marker.CONTENT_URI, // URI
                PROJECTION,                     // Projection
                null,                           // Selection
                null,                           // selection args
                MarkerContract.Marker.COLUMN_NAME_ENTRY_ID // sorting
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mOptionsMenu = menu;
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                SyncUtils.TriggerRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        Cursor c = (Cursor) mAdapter.getItem(position);
        String latLngString = c.getString(COLUMN_LAT) + "," + c.getString(COLUMN_LNG);
        String geoUrlString = "geo:" + latLngString;
        if (geoUrlString == null) {
            Log.e(TAG, "Attempt to launch entry with null link");
            return;
        }

        Log.i(TAG, "Opening URL: " + geoUrlString);
        Uri geoLocation = Uri.parse(geoUrlString);
        Intent i = new Intent(Intent.ACTION_VIEW, geoLocation);
        startActivity(i);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Account account = GenericAccountService.GetAccount(SyncUtils.ACCOUNT_TYPE);
                    if (account == null) {
                        setRefreshActionButtonState(false);
                        return;
                    }

                    boolean syncActive = ContentResolver.isSyncActive(
                            account, MarkerContract.CONTENT_AUTHORITY
                    );
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, MarkerContract.CONTENT_AUTHORITY
                    );
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };
}
