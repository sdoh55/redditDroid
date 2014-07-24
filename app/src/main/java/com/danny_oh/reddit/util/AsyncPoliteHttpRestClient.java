package com.danny_oh.reddit.util;

import android.os.AsyncTask;

import com.github.jreddit.utils.restclient.PoliteHttpRestClient;
import com.github.jreddit.utils.restclient.Response;

/**
 * Created by danny on 7/24/14.
 */
public class AsyncPoliteHttpRestClient extends PoliteHttpRestClient {
    public interface RestClientListener<T> {
        public void onResponse(T object);
    }

    private class getAsync extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            String urlPath = strings[0];
            String cookie = strings[1];

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    @Override
    public Response get(String urlPath, String cookie) {
        return super.get(urlPath, cookie);
    }
}
