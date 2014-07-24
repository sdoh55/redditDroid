package com.danny_oh.reddit;

import android.os.AsyncTask;
import android.util.Log;

import com.github.jreddit.action.MarkActions;
import com.github.jreddit.entity.User;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;

import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * Created by danny on 7/24/14.
 */
public class SessionManager {
    public interface SessionListener<T> {
        public void onResponse(T object);
    }

    User mUser;
    MarkActions mMarkActions;
    RestClient mRestClient;

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
            String fullname = strings[0];
            int dir = Integer.parseInt(strings[1]);
            return mMarkActions.vote(fullname, dir);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            this.mListener.onResponse(aBoolean);
        }
    }

    private static SessionManager mManager;

    public static SessionManager getInstance() {
        if (mManager == null) {
            mManager = new SessionManager(new PoliteHttpRestClient());
        }
        return mManager;
    }

    public SessionManager(RestClient restClient) {
        mRestClient = restClient;
        mMarkActions = new MarkActions(mRestClient);
    }

    public void userLogIn(String username, String password, SessionListener<User> listener) {
        new LoginAsyncTask(listener).execute(username, password);
    }

    public User getUser() { return mUser; }

    public boolean isUserLoggedIn() { return (mUser != null); }


    public void vote(String fullname, int dir, SessionListener<Boolean> listener) {
        new MarkAsyncTask(listener).execute(fullname, Integer.toString(dir));
    }
}
