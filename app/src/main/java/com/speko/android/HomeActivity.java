package com.speko.android;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.FirebaseDatabase;
import com.speko.android.data.User;
import com.speko.android.data.generated.UsersDatabase;
import com.speko.android.sync.SpekoSyncAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static com.speko.android.Utility.getUser;

public class HomeActivity extends AppCompatActivity implements ProfileFragment.OnFragmentInteractionListener {

    // Constants


    private static final int RC_SIGN_IN = 1;
    private static final String SELECTED_ITEM = "arg_selected_item";

    private final String LOG_TAG = getClass().getSimpleName();
    // Instance fields
    Account mAccount;
    private ContentResolver mResolver;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private static UsersDatabase userDB;
    private int mSelectedItem;
    private FirebaseDatabase firebaseDatabase;

    // TODO: Maybe refactor and put it apart because of repeated code in chat activity
    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        private final String LOG_TAG = "BroadcastReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "Intent received in HomeActivity");
            if (intent.getAction().equals(CONNECTIVITY_ACTION)) {
                if (!Utility.isNetworkAvailable(context)) {
                    showSnackBar(true);
                    setActiveConnectivityStatus(context, false);

                } else {
                    showSnackBar(false);
                    setActiveConnectivityStatus(context, true);

                }
            }
        }

        public void setActiveConnectivityStatus(Context c, boolean connectivityStatus) {
            Log.i(LOG_TAG, "Set ConnectivityStatus: " + connectivityStatus);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
            SharedPreferences.Editor spe = sp.edit();
            spe.putBoolean(c.getString(R.string.shared_preference_active_connectivity_status_key), connectivityStatus);
            spe.commit();
        }
    };


    @BindView(R.id.bottom_view_layout_home_activity)
    BottomNavigationView mBottomNavigationView;

    @BindView(R.id.home_activity_coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;
    private Snackbar connectivitySnackBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userDB = UsersDatabase.getInstance(getApplicationContext());
        userDB.onCreate(userDB.getReadableDatabase());

        Fabric.with(this, new Crashlytics());
        Log.d("HomeActivity", "onCreate");

        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        connectivitySnackBar = Snackbar.make(mCoordinatorLayout,
                R.string.connectivity_error, Snackbar.LENGTH_INDEFINITE);

        // Get the content resolver for your app
        mResolver = getContentResolver();

        mFirebaseAuth = FirebaseAuth.getInstance();

        userNotLoggedcheck();
        userNotCreatedCheck();

//        setFireBaseToken();


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            public final String LOG_TAG = getClass().getSimpleName();

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.i(LOG_TAG, "onAuthStateChanged");
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.i(LOG_TAG, "user logged in");

                    // User is signed in
                    //TODO implement this

                    setFireBaseToken();


                } else {
                    Log.i(LOG_TAG, "user logged out");

                    // User is signed out
                    //TODO implement this
                    callLoginActivity();
                }
            }
        };


        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectFragment(item);
                return true;
            }
        });

        MenuItem selectedItem;
        if (savedInstanceState != null) {
            mSelectedItem = savedInstanceState.getInt(SELECTED_ITEM, 0);
            selectedItem = mBottomNavigationView.getMenu().findItem(mSelectedItem);
        } else {
            selectedItem = mBottomNavigationView.getMenu().getItem(0);
        }
        selectFragment(selectedItem);


    }

    private void callLoginActivity() {
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(i);
        finish();
    }


    private void showSnackBar(Boolean show) {
        if (show) {
            connectivitySnackBar.show();
        } else {
            connectivitySnackBar.dismiss();
        }


    }


    private void selectFragment(MenuItem item) {
        Fragment frag = null;
        // init corresponding fragment
        switch (item.getItemId()) {
            case R.id.action_search:
                Log.i(LOG_TAG, "Selecting HomeActivityFragment");

                frag = new HomeActivityFragment();

                break;
            case R.id.action_profile:
                Log.i(LOG_TAG, "Selecting Profile");

                frag = new ProfileFragment();
                Bundle args = new Bundle();
                args.putBoolean(ProfileFragment.BUNDLE_ARGUMENT_IS_SYNCABLE, true);
                args.putBoolean(ProfileFragment.BUNDLE_ARGUMENT_FIRST_TIME_ENABLED, false);
                frag.setArguments(args);
                break;
            case R.id.action_conversations:
                Log.i(LOG_TAG, "Selecting Conversations");
                frag = ConversationsFragment.newInstance(getUser(this).getId());
                break;
        }

        // update selected item
        mSelectedItem = item.getItemId();

        // uncheck the other items.
        for (int i = 0; i < mBottomNavigationView.getMenu().size(); i++) {
            MenuItem menuItem = mBottomNavigationView.getMenu().getItem(i);
            menuItem.setChecked(menuItem.getItemId() == item.getItemId());
        }


        if (frag != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.home_activity_fragment_container, frag, frag.getTag());
            ft.commit();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_ITEM, mSelectedItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        Log.i(LOG_TAG, "onStart");
        userNotCreatedCheck();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangeReceiver, filter);

        super.onStart();
    }

    private void userNotLoggedcheck() {

        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        if (user == null) {
            Log.i(LOG_TAG, "User Not Logged, calling LoginActivity");
            // User is signed in

            callLoginActivity();
        }


    }

    private void userNotCreatedCheck() {
        User user = Utility.getUser(this);
        if(user == null || user.getId() == null || user.getId().isEmpty()){
            callLoginActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }

        }
    }

    private void setFireBaseToken() {
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        user.getToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            public final String LOG_TAG = getClass().getSimpleName();

            @Override
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful()) {
                    String userToken = task.getResult().getToken();
                    Log.i(LOG_TAG, "O token Deu certo! \n");
                    Log.i(LOG_TAG, "O ID do usuário é: \n" + mFirebaseAuth.getCurrentUser().getUid());
                    SpekoSyncAdapter.setUserToken(userToken);
                    SpekoSyncAdapter.initializeSyncAdapter(getApplication());


                } else {
                    Log.e(LOG_TAG, task.getException().getMessage());
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        if (connectivityChangeReceiver != null) {
            unregisterReceiver(connectivityChangeReceiver);
        }

    }


    @Override
    public void completeSignup(User user) {

        final FirebaseUser authUser = mFirebaseAuth.getCurrentUser();

        user.setLearningCode(user.getFluentLanguage()
                + "|"
                + user.getLearningLanguage());

        //adding more Provider User info
        user.setName(authUser.getDisplayName());
        user.setEmail(authUser.getEmail());
        user.setId(authUser.getUid());
        Utility.setUser(user, this);


        Toast.makeText(this, "ProfileUpdated!", Toast.LENGTH_SHORT).show();


    }

}
