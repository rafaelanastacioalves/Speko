package com.speko.android;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.speko.android.data.User;
import com.speko.android.data.UserColumns;
import com.speko.android.data.UsersProvider;

/**
 * Created by rafaelalves on 31/01/17.
 */

public class Utility {

    private static User mUser;
    private static FirebaseDatabase firebaseDatabase;
    private static FirebaseUser authUser;

    private static final String[] USER_COLUMNS = {
            UserColumns.FIREBASE_ID,
            UserColumns.NAME,
            UserColumns.EMAIL
    };


    public void Utility(Context c){


    }

    public static @Nullable User getUser(Context context){
        //TODO should verify if it' null. In case positive should try to retrieve from DB.

        return getUserFromDB(context);

    }

    /**
     * Persists current into Firebase
     * @param user
     * @param c
     */
    public static void setUser(User user, Context c){
        mUser = user;
        authUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseDatabase.getReference()
                .child(c.getString(R.string.firebase_database_node_users))
                .child(authUser.getUid())
                .setValue(user);
    }

    private static User getUserFromDB(Context context){
        Cursor c = context.getContentResolver().query(UsersProvider.Users.USER_URI
        , null, UserColumns.FIREBASE_ID + " = ? ", new String[]{getFirebaseAuthUser().getUid()}, null);
        User user = new User();
        if(c.moveToFirst()){
            user.setLearningLanguage(c.getString(c.getColumnIndex(UserColumns.LEARNING_LANGUAGE)));
            user.setLearningCode(c.getString(c.getColumnIndex(UserColumns.LEARNING_CODE)));
            user.setFluentLanguage(c.getString(c.getColumnIndex(UserColumns.FLUENT_LANGUAGE)));
            user.setId(c.getString(c.getColumnIndex(UserColumns.FIREBASE_ID)));
            user.setName(c.getString(c.getColumnIndex(UserColumns.NAME)));
            user.setEmail(c.getString(c.getColumnIndex(UserColumns.EMAIL)));
            user.setAge(c.getString(c.getColumnIndex(UserColumns.AGE)));

        }


        return user;
    }

    public static CursorLoader getUserFriendsCursorLoader(Context context){
         if (authUser==null){
             authUser = getFirebaseAuthUser();
         }
         return new CursorLoader(context, UsersProvider.Users.usersFrom(authUser.getUid()),
                null,
                null,
                null,
                null);
    }

    private static FirebaseUser getFirebaseAuthUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static Loader<Cursor> getUserCursorLoader(Context context) {
        if (authUser == null) {
            authUser = getFirebaseAuthUser();
        }
        return new CursorLoader(context, UsersProvider.Users.USER_URI
                ,
                null,
                null,
                null,
                null);
    }

}