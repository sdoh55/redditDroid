package com.danny_oh.reddit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.danny_oh.reddit.retrieval.AsyncMarkActions;
import com.danny_oh.reddit.retrieval.AsyncSubmissions;
import com.danny_oh.reddit.util.Constants;
import com.danny_oh.reddit.util.RedditRestClient;
import com.github.jreddit.action.MarkActions;
import com.github.jreddit.action.ProfileActions;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.entity.UserInfo;
import com.github.jreddit.retrieval.Submissions;
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
import java.util.List;

/**
 * Created by danny on 7/24/14.
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
    private AsyncMarkActions mMarkActions;
    private AsyncSubmissions mSubmissionsController;
    private ProfileActions mProfileActions;


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
                ioe.printStackTrace();
                Log.e("SessionManager","IO Exception while attempting to connect user.");
            } catch (ParseException pe) {
                pe.printStackTrace();
                Log.e("SessionManager","Parse Exception while attempting to connect user.");
            } catch (NullPointerException npe) {
                Log.e("SessionManager", "Failed to log in. Likely due to wrong password.");
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
                editor.commit();

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

    private class MarkAsyncTask extends AsyncTask<String, Void, Boolean> {
        private SessionListener<Boolean> mListener;

        public MarkAsyncTask(SessionListener<Boolean> listener) {
            this.mListener = listener;
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            if (isUserLoggedIn()) {
                Log.d("SessionManager", "User is logged in. Voting.");
                String fullname = strings[0];
                int dir = Integer.parseInt(strings[1]);
                return mMarkActions.vote(fullname, dir);
            } else {
                Log.d("SessionManager", "User is not logged in. Returning.");
                ((FragmentActivity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "You need to be logged in to vote.", Toast.LENGTH_SHORT).show();
                    }
                });

                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            this.mListener.onResponse(aBoolean);
        }
    }


    public static class SubmissionFetchParam {
        public String subreddit;
        public SubmissionSort sort;
        public int count;
        public int limit;
        public Submission before;
        public Submission after;
        public boolean show;
    }

    /**
     * AsyncTask subclass that retrieves submissions from a specified subreddit
     */
    private class SubmissionsAsyncTask extends AsyncTask<SubmissionFetchParam, Integer, List<Submission>> {
        private SessionListener<List<Submission>> mListener;

        public SubmissionsAsyncTask(SessionListener<List<Submission>> listener) {
            this.mListener = listener;
        }

        protected List<Submission> doInBackground(SubmissionFetchParam... params) {
            SubmissionFetchParam param = params[0];
            List<Submission> list = mSubmissionsController.ofSubreddit(param.subreddit, param.sort, param.count, param.limit, param.after, param.before, param.show);

            return list;
        }

        protected void onPostExecute(List<Submission> list) {
            mListener.onResponse(list);
        }
    }

    private class ProfileActionsAsynsTask extends AsyncTask<Void, Void, UserInfo> {
        private SessionListener<UserInfo> mListener;

        ProfileActionsAsynsTask(SessionListener<UserInfo> listener) {
            mListener = listener;
        }

        @Override
        protected UserInfo doInBackground(Void... voids) {
            UserInfo info = mProfileActions.getUserInformation();

            return info;
        }

        @Override
        protected void onPostExecute(UserInfo userInfo) {
            mListener.onResponse(userInfo);
        }
    }








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

    public void vote(String fullname, int dir, SessionListener<Boolean> listener) {
        if (isUserLoggedIn()) {
            new MarkAsyncTask(listener).execute(fullname, Integer.toString(dir));
        } else {
            Toast.makeText(mContext, "You need to be logged in to vote.", Toast.LENGTH_SHORT).show();
        }
    }


    public void fetchMoreSubmissions(SubmissionFetchParam param, SessionListener<List<Submission>> listener) {
        new SubmissionsAsyncTask(listener).execute(param);
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
}
