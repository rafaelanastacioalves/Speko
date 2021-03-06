package com.speko.android.data;


import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.OnCreate;
import net.simonvt.schematic.annotation.TableEndpoint;

import static com.speko.android.data.UserContract.CONTENT_AUTHORITY;

/**
 * Created by rafaelalves on 19/12/16.
 */

@SuppressWarnings("ALL")
@ContentProvider(authority = CONTENT_AUTHORITY, database = UsersDatabase.class)
public final class UsersProvider {

    public static final Uri URI
            = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(CONTENT_AUTHORITY).build();

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    private static final String LOG_TAG = "UsersProvider";

    interface Path{
        String USER = "user";
        String FRIENDS = "friends";
        String CHAT_MEMBERS = "chat_members";
        String OTHER_MEMBER = "other_member";

    }

    private static Uri buildUri(String... paths) {
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for (String path : paths) {
            builder.appendPath(path);
        }
        return builder.build();
    }

    @TableEndpoint(table = UsersDatabase.USERS_TABLE) public static class Users {
        @ContentUri(
                path = Path.USER,
                type = "vnd.android.cursor.item/user")
        public static final Uri USER_URI = buildUri(Path.USER);
        @SuppressWarnings("FinalStaticMethod")
        @InexactContentUri(
                name = "FRIENDS_LIST",
                path = Path.USER + "/" + Path.FRIENDS + "/*",
                type = "vnd.android.cursor.dir/friends",
                whereColumn = UserColumns.FRIEND_OF,
                pathSegment = 2
        )
        public static final Uri usersFriendsFrom(String firebaseUserId){
            return buildUri(Path.USER,Path.FRIENDS, firebaseUserId);
        }

        @SuppressWarnings("FinalStaticMethod")
        @InexactContentUri(
                name = "FRIEND",
                path = Path.USER + "/*",
                type = "vnd.android.cursor.item/user",
                whereColumn = UserColumns.FIREBASE_ID,
                pathSegment = 1

        )
        public static final Uri userWith(String firebaseId){
            return buildUri(Path.USER, firebaseId);
        }
    }

    @TableEndpoint(table = UsersDatabase.CHAT_MEMBERS_TABLE) public static class ChatMembers {
            @ContentUri(
                    path = Path.CHAT_MEMBERS,
                    type = "vnd.android.cursor.item/chat_members")
            public static final Uri CHAT_URI = buildUri(Path.CHAT_MEMBERS);
            @SuppressWarnings("FinalStaticMethod")
            @InexactContentUri(
                    name = "FRIENDS_LIST",
                    path = Path.CHAT_MEMBERS+ "/" + Path.OTHER_MEMBER + "/*",
                    type = "vnd.android.cursor.item/chat",
                    whereColumn = ChatMembersColumns.OTHER_MEMBER_ID,
                    pathSegment = 2
            )
            public static final Uri chatInfoWithUser(String firebaseUserId){
                return buildUri(Path.CHAT_MEMBERS,Path.OTHER_MEMBER, firebaseUserId);
            }
        }

    @OnCreate
    public static void onCreate() {

        Log.i(LOG_TAG, "onCreate Provider ");
    }


}
