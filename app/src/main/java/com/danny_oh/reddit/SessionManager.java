package com.danny_oh.reddit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.danny_oh.reddit.retrieval.AsyncComments;
import com.danny_oh.reddit.retrieval.AsyncMarkActions;
import com.danny_oh.reddit.retrieval.AsyncSubmissions;
import com.danny_oh.reddit.util.Constants;
import com.danny_oh.reddit.util.RedditRestClient;
import com.github.jreddit.action.MarkActions;
import com.github.jreddit.action.ProfileActions;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.entity.UserInfo;
import com.github.jreddit.exception.ActionFailedException;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.CommentSort;
import com.github.jreddit.retrieval.params.QuerySyntax;
import com.github.jreddit.retrieval.params.SearchSort;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.retrieval.params.TimeSpan;
import com.github.jreddit.retrieval.params.UserOverviewSort;
import com.github.jreddit.retrieval.params.UserSubmissionsCategory;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;
import com.github.jreddit.utils.restclient.RestResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by danny on 7/24/14.
 *
 * SessionManager is the central class that handles a reddit user session (maintaining user cookies and modhash)
 * and communicating with the reddit service via retrieval helper classes.
 */
public class SessionManager {
    private static final String SHARED_PREFERENCES_USER_FILE = "com.danny_oh.reddit.PREFERENCE_USER_FILE_KEY";
    private static final String SHARED_PREFERENCES_USERNAME = "com.danny_oh.reddit.PREFERENCE_USERNAME_KEY";
    private static final String SHARED_PREFERENCES_USER_COOKIE = "com.danny_oh.reddit.PREFERENCE_USER_COOKIE_KEY";
    private static final String SHARED_PREFERENCES_USER_MODHASH = "com.danny_oh.reddit.PREFERENCE_USER_MODHASH_KEY";

    public interface SessionListener<T> {
        public void onResponse(T object);
    }

    private Context mContext;
    private RestClient mRestClient;
    private User mUser;

    // controllers that start with 'Async' are ones that have been ported from jReddit to be usable on Android
    private AsyncMarkActions mMarkActions;
    private AsyncSubmissions mSubmissionsController;
    private AsyncComments mCommentsController;
    private ProfileActions mProfileActions;


    private static SessionManager mManager;

    public static SessionManager getInstance(Context context) {
        if (mManager == null) {
            mManager = new SessionManager(context, RedditRestClient.getInstance(context));
        }

        return mManager;
    }

    public SessionManager(Context context, RestClient restClient) {
        mContext = context;
        mRestClient = restClient;
        mRestClient.setUserAgent(Constants.USER_AGENT);
        mMarkActions = new AsyncMarkActions((RedditRestClient)mRestClient);
        mSubmissionsController = new AsyncSubmissions((RedditRestClient)mRestClient);
        mProfileActions = new ProfileActions(mRestClient);
        mCommentsController = new AsyncComments((RedditRestClient)mRestClient);



        // restore user info if available
        SharedPreferences preferences = mContext.getSharedPreferences(SHARED_PREFERENCES_USER_FILE, Context.MODE_PRIVATE);
        String username = preferences.getString(SHARED_PREFERENCES_USERNAME, null);
        String cookie = preferences.getString(SHARED_PREFERENCES_USER_COOKIE, null);
        String modhash = preferences.getString(SHARED_PREFERENCES_USER_MODHASH, null);

        if (username != null && cookie != null & modhash != null) {
            mUser = new User(restClient, username, cookie, modhash);
            mProfileActions.switchActor(mUser);
            mMarkActions.switchActor(mUser);
            mSubmissionsController.switchActor(mUser);

            new ProfileActionsAsynsTask(new SessionListener<UserInfo>() {
                @Override
                public void onResponse(UserInfo object) {
                    if (object != null) {
                        Toast.makeText(mContext, "Logged in as " + mUser.getUsername(), Toast.LENGTH_SHORT).show();
                    } else {
                        // TODO: handle failure to get user info on startup (cookie & modhash no longer works)
                        // TODO: need to show logged out screen on main activity drawer. restart activity?
//                        userLogout();
                    }
                }
            }).execute();
        }
    }

    public void userLogIn(String username, String password, SessionListener<User> listener) {
        new LoginAsyncTask(listener).execute(username, password);
    }

    public void userLogout() {
        mUser = null;
        mMarkActions.switchActor(null);
        mSubmissionsController.switchActor(null);
        mProfileActions.switchActor(null);

        // clear saved cookie and modhash
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_USER_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().apply();

        PersistentCookieStore cookieStore = ((RedditRestClient)mRestClient).getCookieStore();
        cookieStore.clear();
    }

    public User getUser() { return mUser; }

    public boolean isUserLoggedIn() { return (mUser != null); }

    public RestClient getRestClient() {
        return mRestClient;
    }

    public void vote(String fullname, int dir, AsyncMarkActions.MarkActionsResponseHandler responseHandler) {
        if (isUserLoggedIn()) {
            mMarkActions.voteAsync(fullname, dir, responseHandler);
        } else {
            Toast.makeText(mContext, "You need to be logged in to vote.", Toast.LENGTH_SHORT).show();
        }
    }

    public void searchSubmissions(String subreddit,
                                  String query,
                                  QuerySyntax syntax,
                                  SearchSort sort,
                                  TimeSpan time,
                                  int count,
                                  int limit,
                                  Submission after,
                                  Submission before,
                                  boolean show_all,
                                  AsyncSubmissions.SubmissionsResponseHandler responseHandler) {

        mSubmissionsController.searchAsync(subreddit, query, syntax, sort, time, count, limit, after, before, show_all, responseHandler);

    }

    public void saveThing(String fullName, AsyncMarkActions.MarkActionsResponseHandler responseHandler) {
        if (isUserLoggedIn()) {
            mMarkActions.saveAsync(fullName, responseHandler);
        } else {
            Toast.makeText(mContext, "You need to be logged in to save links.", Toast.LENGTH_SHORT).show();
        }
    }

    public void unsaveThing(String fullName, AsyncMarkActions.MarkActionsResponseHandler responseHandler) {
        if (isUserLoggedIn()) {
            mMarkActions.unsaveAsync(fullName, responseHandler);
        } else {
            Toast.makeText(mContext, "You need to be logged in to unsave links.", Toast.LENGTH_SHORT).show();
        }
    }

    public void getUserSubmissions(UserSubmissionsCategory category, UserOverviewSort sort, int count, int limit, Submission after, Submission before, boolean show_given, AsyncSubmissions.SubmissionsResponseHandler handler) {
        if (isUserLoggedIn())
            mSubmissionsController.ofUserAsync(mUser.getUsername(), category, sort, count, limit, after, before, show_given, handler);
    }

    public void getSubredditSubmissions(String subreddit, SubmissionSort sort, int count, int limit, Submission after, Submission before, boolean show_all, AsyncSubmissions.SubmissionsResponseHandler handler) {
        mSubmissionsController.ofSubredditAsync(subreddit, sort, count, limit, after, before, show_all, handler);
    }

    public void getCommentsFromSubmission(Submission submission, String commentId, int parentsShown, int depth, int limit, CommentSort sort, AsyncComments.CommentsResponseHandler responseHandler) {
        mCommentsController.ofSubmissionAsync(submission, commentId, parentsShown, depth, limit, sort, responseHandler);
    }

    public void getMoreChildren(Submission submission, List<String> children, CommentSort sort, AsyncComments.CommentsResponseHandler responseHandler) {
        mCommentsController.moreChildrenAsync(submission, children, sort, responseHandler);
    }


/*
 * these AsyncTasks are temporary wrappers for jReddit (Java based library) to be usable on Android
 * and should be reimplemented soon.
 */
    private class LoginAsyncTask extends AsyncTask<String, Void, User> {
        SessionListener<User> mListener;

        public LoginAsyncTask(SessionListener listener) {
            this.mListener = listener;
        }

        @Override
        protected User doInBackground(String... strings) {
            String username = strings[0];
            String password = strings[1];

            mUser = new User(mRestClient, username);
            try {
                mUser.connect(password, true);
            } catch (IOException ioe) {
                Log.e("SessionManager","IO Exception while attempting to connect user. Localized message: " + ioe.getLocalizedMessage());
            } catch (ParseException pe) {
                Log.e("SessionManager","Parse Exception while attempting to connect user. Localized message: " + pe.getLocalizedMessage());
            } catch (NullPointerException npe) {
                Log.e("SessionManager", "Failed to log in. Likely due to wrong password. Localized message: " + npe.getLocalizedMessage());
                return null;
            }

            return mUser;
        }

        @Override
        protected void onPostExecute(User user) {
            if (user != null) {

                SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_USER_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putString(SHARED_PREFERENCES_USERNAME, user.getUsername());
                editor.putString(SHARED_PREFERENCES_USER_COOKIE, user.getCookie());
                editor.putString(SHARED_PREFERENCES_USER_MODHASH, user.getModhash());
                editor.apply();

                mMarkActions.switchActor(user);
                mSubmissionsController.switchActor(user);
                mProfileActions.switchActor(user);
                mListener.onResponse(user);
            } else {
                ((FragmentActivity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "Wrong password. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    /*
     * temporary AsyncTask that verifies if a logged in user session is valid
     */
    private class ProfileActionsAsynsTask extends AsyncTask<Void, Void, UserInfo> {
        private SessionListener<UserInfo> mListener;

        ProfileActionsAsynsTask(SessionListener<UserInfo> listener) {
            mListener = listener;
        }

        @Override
        protected UserInfo doInBackground(Void... voids) {
            try {
                return mProfileActions.getUserInformation();
            } catch (ActionFailedException e) {
                Log.e("SessionManager", "Failed to retrieve user info. Localized message: " + e.getLocalizedMessage());

                return null;
            }
        }

        @Override
        protected void onPostExecute(UserInfo userInfo) {
            mListener.onResponse(userInfo);
        }
    }



}
