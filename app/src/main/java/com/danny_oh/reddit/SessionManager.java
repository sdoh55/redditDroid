package com.danny_oh.reddit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.github.jreddit.action.MarkActions;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;

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
    private MarkActions mMarkActions;
    private Submissions mSubmissionsController;

    private class LoginAsyncTask extends AsyncTask<String, Void, User> {
        SessionListener<User> mListener;

        public LoginAsyncTask(SessionListener listener) {
            this.mListener = listener;
        }

        @Override
        protected User doInBackground(String... strings) {
            String username = strings[0];
            String password = strings[1];

            mUser = new User(mRestClient, username, password);
            try {
                mUser.connect();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.e("SessionManager","IO Exception while attempting to connect user.");
            } catch (ParseException pe) {
                pe.printStackTrace();
                Log.e("SessionManager","Parse Exception while attempting to connect user.");
            }

            return mUser;
        }

        @Override
        protected void onPostExecute(User user) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_USER_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString(SHARED_PREFERENCES_USERNAME, user.getUsername());
            editor.putString(SHARED_PREFERENCES_USER_COOKIE, user.getCookie());
            editor.putString(SHARED_PREFERENCES_USER_MODHASH, user.getModhash());
            editor.commit();

            mMarkActions.switchActor(user);
            mSubmissionsController.switchActor(mUser);
            mListener.onResponse(user);
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

    private static SessionManager mManager;

    public static SessionManager getInstance(Context context) {
        if (mManager == null) {// if vote was successful
            mManager = new SessionManager(context, new HttpRestClient());
        }
        return mManager;
    }

    public SessionManager(Context context, RestClient restClient) {
        mContext = context;
        mRestClient = restClient;
        mMarkActions = new MarkActions(mRestClient);
        mSubmissionsController = new Submissions(mRestClient);


        // restore user info if available
        SharedPreferences preferences = mContext.getSharedPreferences(SHARED_PREFERENCES_USER_FILE, Context.MODE_PRIVATE);
        String username = preferences.getString(SHARED_PREFERENCES_USERNAME, null);
        String cookie = preferences.getString(SHARED_PREFERENCES_USER_COOKIE, null);
        String modhash = preferences.getString(SHARED_PREFERENCES_USER_MODHASH, null);

        if (username != null && cookie != null & modhash != null) {
            mUser = new User(username, cookie, modhash);
            mMarkActions.switchActor(mUser);
            mSubmissionsController.switchActor(mUser);
            Toast.makeText(mContext, "Logged in as " + mUser.getUsername(), Toast.LENGTH_SHORT).show();
        }
    }

    public void userLogIn(String username, String password, SessionListener<User> listener) {
        new LoginAsyncTask(listener).execute(username, password);
    }

    public void userLogout() {
        mUser = null;

        // clear saved cookie and modhash
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_USER_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().commit();
    }

    public User getUser() { return mUser; }

    public boolean isUserLoggedIn() { return (mUser != null); }


    public void vote(String fullname, int dir, SessionListener<Boolean> listener) {
        new MarkAsyncTask(listener).execute(fullname, Integer.toString(dir));
    }


    public void fetchMoreSubmissions(SubmissionFetchParam param, SessionListener<List<Submission>> listener) {
        new SubmissionsAsyncTask(listener).execute(param);
    }
}
