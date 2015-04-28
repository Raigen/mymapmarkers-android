package com.foellerich.mymapmarkers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.example.android.common.accounts.GenericAccountService;
import com.foellerich.mymapmarkers.provider.MarkerContract;

/**
 * Created by foellerich on 25.04.2015.
 */
public class SyncUtils {
    private static final long SYNC_FREQUENCY = 60 * 60; // 1 hour in seconds
    private static final String CONTENT_AUTHORITY = MarkerContract.CONTENT_AUTHORITY;
    private static final String PREF_SETUP_COMPLETE = "setup_complete";
    public static final String ACCOUNT_TYPE = "com.foellerich.mymapmarkers.account";

    /**
     * create an entry for this application in the system account list, if not already there.
     * @param context Context
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static void CreateSyncAccount(Context context) {
        boolean newAccount = false;
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);

        // create account if missing
        Account account = GenericAccountService.GetAccount(ACCOUNT_TYPE);
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(
                    account, CONTENT_AUTHORITY, new Bundle(), SYNC_FREQUENCY
            );
            newAccount = true;
        }

        if (newAccount || !setupComplete) {
            TriggerRefresh();
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }
    }

    public static void TriggerRefresh() {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                GenericAccountService.GetAccount(ACCOUNT_TYPE), // sync this account
                MarkerContract.CONTENT_AUTHORITY,                 // with this content authority
                b                                               // extra options
        );
    }
}
