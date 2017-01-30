package com.speko.android.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.speko.android.R;
import com.speko.android.data.User;
import com.speko.android.data.UserColumns;
import com.speko.android.retrofit.AccessToken;
import com.speko.android.retrofit.FirebaseClient;
import com.speko.android.retrofit.ServiceGenerator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import retrofit2.Call;

import static android.content.Context.ACCOUNT_SERVICE;
import static com.speko.android.data.UserColumns.FIREBASE_ID;
import static com.speko.android.data.UserContract.ACCOUNT_TYPE;
import static com.speko.android.data.UserContract.AUTHORITY;
import static com.speko.android.data.UsersProvider.Users.USER_URI;
import static com.speko.android.sync.SpekoAuthenticator.ACCOUNT;


/**
 * Created by rafaelalves on 14/12/16.
 */
public class SpekoSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final int SYNC_INTERVAL = 1000; //every minute
    private static final int FLEX_TIME = 1000; // every minute
    private static FirebaseDatabase mFirebaseDatabase;
    private static FirebaseAuth mFirebaseAuth;
    private static final String LOG_TAG = "SpekoSyncAdapter";
    private static String userToken;
    private static User user;


    public SpekoSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.i(LOG_TAG, "Constructor Called");

    }

    public SpekoSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        Log.i(LOG_TAG, "Constructor Called");

    }

    public static void setUserToken(String userToken) {
        SpekoSyncAdapter.userToken = userToken;
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync");


        if (userToken != null) {
            User user = getUser(userToken);
            persistUser(user);

            HashMap<String, User> userFriends = null;
            try {
                userFriends = getFriends(userToken);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (userFriends != null) {
                persistFriends(userFriends.values().toArray(new User[userFriends.size()]));
                Log.i("SpekoSyncAdapter", "Deu certo!: \n" + user.toString());

            }


        } else {
            Log.w(LOG_TAG, "userToken not setted!");
        }


    }

    private void persistFriends(User[] userFriends) {


        int count = 0;
        for (User user :
                userFriends) {
            ContentValues userCV = new ContentValues();
            userCV.put(UserColumns.FIREBASE_ID, user.getId());
            userCV.put(UserColumns.NAME, user.getName());
            userCV.put(UserColumns.EMAIL, user.getEmail());
            userCV.put(UserColumns.FLUENT_LANGUAGE, user.getFluentLanguage());
            userCV.put(UserColumns.LEARNING_LANGUAGE, user.getLearningLanguage());
            userCV.put(UserColumns.FRIEND_OF, mFirebaseAuth.getCurrentUser().getUid());

            try {

                Log.d(LOG_TAG, "trying to insert a row...");
                //noinspection UnusedAssignment
                Uri rowNumber = getContext().getContentResolver().insert(USER_URI, userCV);
                Log.d(LOG_TAG, "inserted ok! Count: " + (count + 1));

            } catch (Exception e) {
                Log.e(LOG_TAG, "Insert not possible:" + e.getCause());
                Log.d(LOG_TAG, "Trying to update");
                Log.d(LOG_TAG, "Values: " + user.getId() + " "
                        + user.getName() + " "
                        + user.getEmail());
                int rows = getContext().getContentResolver().update(USER_URI, userCV,
                        UserColumns.FIREBASE_ID + " = ?", new String[]{user.getId()});
                if (rows > 0) {
                    Log.i(LOG_TAG, "updated successfuly. Count: " + (count + 1));
                }
            }

        }

    }

    private HashMap<String, User> getFriends(String idToken) throws UnsupportedEncodingException {

        // Fetch and print a list of the contributors to this library.
        FirebaseClient client = ServiceGenerator.createService(FirebaseClient.class, new AccessToken(
                "Bearer",
                idToken)
        );


        Call<HashMap<String,User>> call = client.getUsersListWith(userToken
                ,"\"fluentLanguage\""
                , "\"" + user.getLearningLanguage() + "\"");

        try {
            Log.i("SpekoSyncAdapter", "getFriends: \n");
            HashMap<String, User> friends = call.execute().body();

            return friends;
        } catch (IOException e) {
            Log.e("SpekoSyncAdapter", "Deu ruim: \n" + e.getMessage());
            // handle errors
        }

        return null;


    }

    private void persistUser(User user) {
        ContentValues userCV = new ContentValues();
        userCV.put(FIREBASE_ID, user.getId());
        userCV.put(UserColumns.NAME, user.getName());
        userCV.put(UserColumns.EMAIL, user.getEmail());
        userCV.put(UserColumns.FLUENT_LANGUAGE, user.getFluentLanguage());
        userCV.put(UserColumns.LEARNING_LANGUAGE, user.getLearningLanguage());

        // deleting any row first
        getContext().getContentResolver().delete(USER_URI,
                FIREBASE_ID + " = ?",
                new String[]{user.getId()});

        // insterting
        getContext().getContentResolver().insert(USER_URI, userCV);
    }

    private static User getUser(String idToken) {
        // Fetch and print a list of the contributors to this library.
        FirebaseClient client = ServiceGenerator.createService(FirebaseClient.class, new AccessToken(
                "Bearer",
                idToken)
        );
        Call<User> call = client.getUser(mFirebaseAuth.getCurrentUser().getUid(), idToken);

        try {
            Log.i("SpekoSyncAdapter", "getUser: \n");
            user = call.execute().body();
            Log.i("SpekoSyncAdapter", "Deu certo!: \n" + user.toString());

            return user;
        } catch (IOException e) {
            Log.e("SpekoSyncAdapter", "Deu ruim: \n" + e.getMessage());
            // handle errors
        }

        return null;


    }

    public static void initializeSyncAdapter(Context context) {

        Log.d("SpekoSyncAdapter", "initializeSyncAdapter");
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        getSyncAccount(context);


    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account getSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, CONTENT_AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);


        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */

            Log.w("HomeActivity", "Deu ruim com o Account");
            return null;
        }

        return newAccount;

    }

    private static void onAccountCreated(Account newAccount, Context context) {

        configurePeriodicSync(context, SYNC_INTERVAL, FLEX_TIME);
        // Inform the system that this account is eligible for auto sync when the network is up
        ContentResolver.setSyncAutomatically(newAccount, AUTHORITY, true);

//        TODO Uncomment this:
//        syncImmediatly(context);
    }

    public static void syncImmediatly(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                AUTHORITY, bundle);
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    private static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (account != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // we can enable inexact timers in our periodic sync
                SyncRequest request = new SyncRequest.Builder().
                        syncPeriodic(syncInterval, flexTime).
                        setSyncAdapter(account, authority).
                        setExtras(new Bundle()).build();
                Log.i(LOG_TAG, "request: " + request.toString());

                ContentResolver.requestSync(request);
            } else {
                ContentResolver.addPeriodicSync(account,
                        authority, new Bundle(), syncInterval);
            }
        } else {
            Log.i(LOG_TAG, "Account retornando null!");

        }

    }

}
